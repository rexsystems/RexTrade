package me.rexsystems.rexTrade.trade;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.rexsystems.rexTrade.RexTrade;
import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.config.Settings;
import me.rexsystems.rexTrade.config.SoundSpec;
import me.rexsystems.rexTrade.currency.Ledgers;
import me.rexsystems.rexTrade.currency.VaultHook;
import me.rexsystems.rexTrade.item.ItemFit;
import me.rexsystems.rexTrade.money.MoneyExchange;
import me.rexsystems.rexTrade.money.Tax;
import me.rexsystems.rexTrade.util.MessageService;
import me.rexsystems.rexTrade.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeManager {

    public enum CancelReason {
        CLOSE,
        QUIT,
        DEATH,
        WORLD,
        DISTANCE,
        GAMEMODE,
        DISABLE
    }

    private final RexTrade plugin;
    private final ItemReturn returns;
    private final TradeLog log;
    private final Object lock = new Object();
    private final Map<UUID, TradeSession> byPlayer = new LinkedHashMap<>();
    private final Map<UUID, TradeSession> byId = new LinkedHashMap<>();
    private final Map<UUID, Request> incoming = new LinkedHashMap<>();
    private final Map<UUID, UUID> outgoing = new LinkedHashMap<>();

    public TradeManager(RexTrade plugin) {
        this.plugin = plugin;
        this.returns = new ItemReturn(plugin);
        this.log = new TradeLog(plugin);
    }

    public TradeSession session(Player player) {
        if (player == null) {
            return null;
        }
        synchronized (lock) {
            return byPlayer.get(player.getUniqueId());
        }
    }

    public TradeSession session(UUID sessionId) {
        synchronized (lock) {
            return byId.get(sessionId);
        }
    }

    public void request(Player sender, Player target) {
        Settings settings = plugin.getConfigManager().settings();
        if (!settings.enabled()) {
            tell(sender, settings, "disabled", Map.of());
            return;
        }
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            tell(sender, settings, "request-self", Map.of());
            return;
        }
        if (!allowed(sender, sender, settings)) {
            return;
        }
        Location from = sender.getLocation().clone();
        String senderWorld = sender.getWorld() == null ? "" : sender.getWorld().getName();
        SchedulerUtils.runForPlayer(plugin, target, () -> finishRequest(sender, target, from, senderWorld, settings));
    }

    private void finishRequest(Player sender, Player target, Location from, String senderWorld, Settings settings) {
        if (!sender.isOnline() || !target.isOnline()) {
            return;
        }
        if (!allowed(target, sender, settings)) {
            return;
        }
        if (!target.hasPermission("rextrade.use")) {
            tell(sender, settings, "request-no-target-permission", Map.of("partner", target.getName()));
            return;
        }
        if (senderWorld.isEmpty() || target.getWorld() == null || !senderWorld.equals(target.getWorld().getName())) {
            tell(sender, settings, "request-world", Map.of());
            return;
        }
        if (tooFar(from, target.getLocation(), settings.maxDistance())) {
            tell(sender, settings, "request-distance", Map.of("distance", distanceText(settings.maxDistance())));
            return;
        }
        synchronized (lock) {
            if (byPlayer.containsKey(sender.getUniqueId()) || byPlayer.containsKey(target.getUniqueId())) {
                tell(sender, settings, "request-busy", Map.of("partner", target.getName()));
                return;
            }
            dropRequest(sender.getUniqueId());
            Request previous = incoming.get(target.getUniqueId());
            if (previous != null) {
                cancelTask(previous.task);
                outgoing.remove(previous.sender, target.getUniqueId());
            }
            Request request = new Request(UUID.randomUUID(), sender.getUniqueId(), target.getUniqueId(), settings.requestTimeoutSeconds(), settings.maxDistance());
            incoming.put(target.getUniqueId(), request);
            outgoing.put(sender.getUniqueId(), target.getUniqueId());
            long delay = Math.max(1L, request.timeoutSeconds * 20L);
            request.task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduled -> expire(request.id), delay);
        }
        Map<String, String> placeholders = Map.of(
                "player", sender.getName(),
                "partner", target.getName(),
                "seconds", Integer.toString(settings.requestTimeoutSeconds())
        );
        tell(sender, settings, "request-sent", placeholders);
        tell(target, settings, "request-received", placeholders);
        play(sender, settings, "request");
        play(target, settings, "request");
    }

    public void accept(Player target) {
        Settings settings = plugin.getConfigManager().settings();
        if (!target.hasPermission("rextrade.use")) {
            tell(target, settings, "no-permission", Map.of());
            return;
        }
        if (!settings.enabled()) {
            tell(target, settings, "disabled", Map.of());
            return;
        }
        Request request;
        synchronized (lock) {
            request = incoming.remove(target.getUniqueId());
            if (request != null) {
                outgoing.remove(request.sender, target.getUniqueId());
                cancelTask(request.task);
            }
        }
        if (request == null) {
            tell(target, settings, "request-none", Map.of());
            return;
        }
        Player sender = Bukkit.getPlayer(request.sender);
        if (sender == null || !sender.isOnline()) {
            tell(target, settings, "request-expired", Map.of());
            return;
        }
        if (!allowed(target, target, settings)) {
            return;
        }
        Location targetLocation = target.getLocation().clone();
        String targetWorld = target.getWorld() == null ? "" : target.getWorld().getName();
        SchedulerUtils.runForPlayer(plugin, sender, () -> finishAccept(request, sender, target, targetLocation, targetWorld, settings));
    }

    private void finishAccept(Request request, Player sender, Player target, Location targetLocation, String targetWorld, Settings settings) {
        if (!sender.isOnline() || !target.isOnline()) {
            tell(target, settings, "request-expired", Map.of());
            return;
        }
        if (!allowed(sender, target, settings)) {
            return;
        }
        if (sender.getWorld() == null || !sender.getWorld().getName().equals(targetWorld)) {
            tell(target, settings, "request-world", Map.of());
            return;
        }
        if (tooFar(sender.getLocation(), targetLocation, request.maxDistance)) {
            tell(target, settings, "request-distance", Map.of("distance", distanceText(request.maxDistance)));
            return;
        }
        TradeSession session = new TradeSession(sender.getUniqueId(), target.getUniqueId(), sender.getName(), target.getName(), settings);
        session.remember(sender.getUniqueId(), sender.getLocation());
        session.remember(target.getUniqueId(), targetLocation);
        synchronized (lock) {
            if (byPlayer.containsKey(sender.getUniqueId()) || byPlayer.containsKey(target.getUniqueId())) {
                tell(target, settings, "request-busy", Map.of("partner", sender.getName()));
                return;
            }
            byPlayer.put(sender.getUniqueId(), session);
            byPlayer.put(target.getUniqueId(), session);
            byId.put(session.id(), session);
        }
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> {
            if (!session.isOpen()) {
                scheduled.cancel();
                return;
            }
            checkDistance(session);
        }, 20L, 20L);
        session.distanceTask(task);
        open(session, sender, target.getName());
        open(session, target, sender.getName());
        tell(sender, settings, "session-open", Map.of("partner", target.getName()));
        tell(target, settings, "session-open", Map.of("partner", sender.getName()));
        play(sender, settings, "accept");
        play(target, settings, "accept");
    }

    public void deny(Player target) {
        Settings settings = plugin.getConfigManager().settings();
        Request request;
        synchronized (lock) {
            request = incoming.remove(target.getUniqueId());
            if (request != null) {
                outgoing.remove(request.sender, target.getUniqueId());
                cancelTask(request.task);
            }
        }
        if (request == null) {
            tell(target, settings, "request-none", Map.of());
            return;
        }
        tell(target, settings, "request-denied", Map.of());
        Player sender = Bukkit.getPlayer(request.sender);
        if (sender != null) {
            tell(sender, settings, "request-denied-sender", Map.of("partner", target.getName()));
            play(sender, settings, "deny");
        }
        play(target, settings, "deny");
    }

    public void offer(Player player, String currencyId, BigDecimal amount) {
        TradeSession session = session(player);
        Settings settings = session == null ? plugin.getConfigManager().settings() : session.rules();
        if (session == null) {
            tell(player, settings, "not-in-trade", Map.of());
            return;
        }
        CurrencySpec currency = settings.currency(currencyId);
        if (currency == null) {
            tell(player, settings, "currency-unknown", Map.of("currency", currencyId == null ? "" : currencyId));
            return;
        }
        if (!session.setMoney(player.getUniqueId(), currency.id(), amount)) {
            tell(player, settings, "not-in-trade", Map.of());
            return;
        }
        tell(player, settings, "offer-set", Map.of(
                "currency", currency.name(),
                "amount", me.rexsystems.rexTrade.util.CompactNumbers.format(amount, settings.formatMode())
        ));
        refresh(session);
    }

    public void cancelClosed(Player player) {
        end(player, CancelReason.CLOSE, true);
    }

    public void cancelQuit(Player player) {
        end(player, CancelReason.QUIT, true);
    }

    public void cancelWorld(Player player) {
        end(player, CancelReason.WORLD, true);
    }

    public void cancelGameMode(Player player) {
        end(player, CancelReason.GAMEMODE, true);
    }

    public void cancelDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        TradeSession session = session(player);
        if (session == null) {
            return;
        }
        TradeSession.ItemBundle bundle = session.cancelOrDefer();
        if (bundle == null) {
            return;
        }
        untrack(session);
        List<ItemStack> own = bundle.forPlayer(player.getUniqueId());
        if (event.getKeepInventory()) {
            returns.now(player, own);
        } else {
            for (ItemStack stack : own) {
                if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                    event.getDrops().add(stack);
                }
            }
        }
        UUID partnerId = session.partner(player.getUniqueId());
        Player partner = Bukkit.getPlayer(partnerId);
        if (partner != null) {
            returns.later(partner, bundle.forPlayer(partnerId));
            tell(partner, session.rules(), "partner-died", Map.of("partner", player.getName()));
            play(partner, session.rules(), "cancel");
            closeView(partner, session);
        } else {
            returns.dropAt(session.location(partnerId), bundle.forPlayer(partnerId));
        }
        closeView(player, session);
    }

    public void shutdown() {
        List<TradeSession> sessions;
        synchronized (lock) {
            sessions = new ArrayList<>(new LinkedHashSet<>(byPlayer.values()));
            byPlayer.clear();
            byId.clear();
            for (Request request : incoming.values()) {
                cancelTask(request.task);
            }
            incoming.clear();
            outgoing.clear();
        }
        for (TradeSession session : sessions) {
            TradeSession.ItemBundle bundle = session.forceClose();
            if (bundle == null) {
                continue;
            }
            Player first = Bukkit.getPlayer(bundle.first());
            Player second = Bukkit.getPlayer(bundle.second());
            if (first != null) {
                returns.now(first, bundle.firstItems());
            } else {
                returns.dropAt(session.location(bundle.first()), bundle.firstItems());
            }
            if (second != null) {
                returns.now(second, bundle.secondItems());
            } else {
                returns.dropAt(session.location(bundle.second()), bundle.secondItems());
            }
        }
        returns.flush();
    }

    public void refresh(TradeSession session) {
        Player first = Bukkit.getPlayer(session.playerA());
        Player second = Bukkit.getPlayer(session.playerB());
        if (first != null) {
            SchedulerUtils.runForPlayer(plugin, first, () -> TradeMenus.render(session, first.getUniqueId()));
        }
        if (second != null) {
            SchedulerUtils.runForPlayer(plugin, second, () -> TradeMenus.render(session, second.getUniqueId()));
        }
    }

    public void refreshActor(TradeSession session, Player actor) {
        TradeMenus.render(session, actor.getUniqueId());
        Player partner = Bukkit.getPlayer(session.partner(actor.getUniqueId()));
        if (partner != null) {
            SchedulerUtils.runForPlayer(plugin, partner, () -> TradeMenus.render(session, partner.getUniqueId()));
        }
    }

    public void startConfirm(TradeSession session) {
        Player left = Bukkit.getPlayer(session.playerA());
        Player right = Bukkit.getPlayer(session.playerB());
        if (left == null || right == null) {
            TradeSession.ItemBundle bundle = session.forceClose();
            if (bundle != null) {
                untrack(session);
                giveBack(session, bundle, null);
                tellBoth(session, "partner-left", "partner-left");
                closeBoth(session);
            }
            return;
        }
        SchedulerUtils.runForPlayer(plugin, left, () -> finishFit(session, true, left, right), () -> {
            session.requestAbort();
            if (session.reportFit(true, false)) {
                afterFit(session);
            }
        });
        SchedulerUtils.runForPlayer(plugin, right, () -> finishFit(session, false, right, left), () -> {
            session.requestAbort();
            if (session.reportFit(false, false)) {
                afterFit(session);
            }
        });
    }

    private void finishFit(TradeSession session, boolean sideA, Player self, Player other) {
        boolean fits = ItemFit.canFit(self.getInventory().getStorageContents(), session.copyItems(other.getUniqueId()));
        if (session.reportFit(sideA, fits)) {
            afterFit(session);
        }
    }

    private void afterFit(TradeSession session) {
        if (session.abortRequested()) {
            TradeSession.ItemBundle bundle = session.forceClose();
            if (bundle != null) {
                untrack(session);
                giveBack(session, bundle, null);
                tellBoth(session, "cancelled", "cancelled");
                closeBoth(session);
            }
            return;
        }
        if (!session.bothFit()) {
            session.abortConfirm();
            Player left = Bukkit.getPlayer(session.playerA());
            Player right = Bukkit.getPlayer(session.playerB());
            if (left != null) {
                tell(left, session.rules(), "no-space", Map.of("partner", session.nameOf(session.playerB())));
            }
            if (right != null) {
                tell(right, session.rules(), "no-space", Map.of("partner", session.nameOf(session.playerA())));
            }
            refresh(session);
            return;
        }
        SchedulerUtils.runSync(plugin, () -> settle(session));
    }

    private void settle(TradeSession session) {
        if (!session.isConfirming()) {
            return;
        }
        if (session.abortRequested()) {
            TradeSession.ItemBundle bundle = session.forceClose();
            if (bundle != null) {
                untrack(session);
                giveBack(session, bundle, null);
                tellBoth(session, "cancelled", "cancelled");
                closeBoth(session);
            }
            return;
        }
        Player left = Bukkit.getPlayer(session.playerA());
        Player right = Bukkit.getPlayer(session.playerB());
        if (left == null || right == null) {
            session.requestAbort();
            TradeSession.ItemBundle bundle = session.forceClose();
            if (bundle != null) {
                untrack(session);
                giveBack(session, bundle, null);
            }
            return;
        }
        List<MoneyExchange.Leg> legs = legs(session, left, right);
        Map<String, BigDecimal> leftMoney = session.copyMoney(left.getUniqueId());
        Map<String, BigDecimal> rightMoney = session.copyMoney(right.getUniqueId());
        TradeSession.Finish finish = session.finish(legs);
        if (finish.skipped()) {
            return;
        }
        if (!finish.completed()) {
            tell(left, session.rules(), "money-failed", Map.of());
            tell(right, session.rules(), "money-failed", Map.of());
            if (needsVault(session) && VaultHook.economy() == null) {
                tell(left, session.rules(), "vault-missing", Map.of());
                tell(right, session.rules(), "vault-missing", Map.of());
            }
            refresh(session);
            return;
        }
        untrack(session);
        TradeSession.ItemBundle bundle = finish.bundle();
        returns.later(left, bundle.secondItems());
        returns.later(right, bundle.firstItems());
        writeLog(session, left, right, bundle, leftMoney, rightMoney);
        play(left, session.rules(), "complete");
        play(right, session.rules(), "complete");
        closeBoth(session);
    }

    private void end(Player actor, CancelReason reason, boolean actorIsOnline) {
        if (actor == null) {
            return;
        }
        TradeSession session = session(actor);
        if (session == null) {
            return;
        }
        TradeSession.ItemBundle bundle = session.cancelOrDefer();
        if (bundle == null) {
            return;
        }
        untrack(session);
        UUID actorId = actorIsOnline ? actor.getUniqueId() : null;
        giveBack(session, bundle, actorId);
        Settings rules = session.rules();
        Player partner = Bukkit.getPlayer(session.partner(actor.getUniqueId()));
        switch (reason) {
            case QUIT -> {
                if (partner != null) {
                    tell(partner, rules, "partner-left", Map.of("partner", actor.getName()));
                }
            }
            case WORLD -> tellBoth(session, "world-change", "world-change");
            case DISTANCE -> tellBoth(session, "too-far", "too-far");
            case GAMEMODE -> tellBoth(session, "cancelled", "cancelled");
            default -> {
                tell(actor, rules, "cancelled", Map.of());
                if (partner != null) {
                    tell(partner, rules, "cancelled", Map.of());
                }
            }
        }
        play(actor, rules, "cancel");
        if (partner != null) {
            play(partner, rules, "cancel");
        }
        closeBoth(session);
    }

    private void abandon(TradeSession session) {
        TradeSession.ItemBundle bundle = session.forceClose();
        if (bundle == null) {
            return;
        }
        untrack(session);
        giveBack(session, bundle, null);
    }

    private void giveBack(TradeSession session, TradeSession.ItemBundle bundle, UUID nowPlayer) {
        giveOne(session, bundle.first(), bundle.firstItems(), nowPlayer);
        giveOne(session, bundle.second(), bundle.secondItems(), nowPlayer);
    }

    private void giveOne(TradeSession session, UUID playerId, List<ItemStack> items, UUID nowPlayer) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && playerId.equals(nowPlayer)) {
            returns.now(player, items);
            return;
        }
        if (player != null) {
            returns.later(player, items);
            return;
        }
        returns.dropAt(session.location(playerId), items);
    }

    private List<MoneyExchange.Leg> legs(TradeSession session, Player left, Player right) {
        List<MoneyExchange.Leg> legs = new ArrayList<>();
        Settings rules = session.rules();
        for (CurrencySpec currency : rules.currencies()) {
            BigDecimal leftOffer = session.money(left.getUniqueId(), currency.id());
            BigDecimal rightOffer = session.money(right.getUniqueId(), currency.id());
            if (leftOffer.signum() == 0 && rightOffer.signum() == 0) {
                continue;
            }
            legs.add(new MoneyExchange.Leg(
                    Ledgers.forPlayer(left, currency),
                    Ledgers.forPlayer(right, currency),
                    leftOffer,
                    rightOffer,
                    Tax.payout(rightOffer, rules.taxPercent()),
                    Tax.payout(leftOffer, rules.taxPercent())
            ));
        }
        return legs;
    }

    private void summary(TradeSession session, Player viewer, Player partner, Map<String, BigDecimal> sent,
                         Map<String, BigDecimal> receivedOffers, List<ItemStack> sentItems, List<ItemStack> receivedItems) {
        Settings rules = session.rules();
        Map<String, BigDecimal> received = new LinkedHashMap<>();
        for (CurrencySpec currency : rules.currencies()) {
            received.put(currency.id(), Tax.payout(receivedOffers.getOrDefault(currency.id(), BigDecimal.ZERO), rules.taxPercent()));
        }
        Map<String, String> placeholders = Map.of(
                "partner", partner.getName(),
                "sent_items", TradeText.items(sentItems),
                "received_items", TradeText.items(receivedItems),
                "sent_currency", TradeText.money(rules, sent),
                "received_currency", TradeText.money(rules, received),
                "tax", TradeText.tax(rules, sent, receivedOffers)
        );
        for (String line : rules.summary()) {
            String rendered = line.replace("<prefix>", rules.prefix()).replace("{prefix}", rules.prefix());
            SchedulerUtils.runForPlayer(plugin, viewer, () -> MessageService.send(viewer, rendered, placeholders));
        }
    }

    private void writeLog(TradeSession session, Player left, Player right, TradeSession.ItemBundle bundle,
                          Map<String, BigDecimal> leftMoney, Map<String, BigDecimal> rightMoney) {
        Settings rules = session.rules();
        String line = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                + " " + left.getName()
                + " gave " + TradeText.items(bundle.firstItems())
                + " and " + TradeText.money(rules, leftMoney)
                + " | " + right.getName()
                + " gave " + TradeText.items(bundle.secondItems())
                + " and " + TradeText.money(rules, rightMoney)
                + " | tax " + rules.taxPercent().stripTrailingZeros().toPlainString() + "%";
        log.append(line);
        summary(session, left, right, leftMoney, rightMoney, bundle.firstItems(), bundle.secondItems());
        summary(session, right, left, rightMoney, leftMoney, bundle.secondItems(), bundle.firstItems());
    }

    private boolean needsVault(TradeSession session) {
        for (CurrencySpec currency : session.rules().currencies()) {
            if (!currency.placeholder()
                    && (session.money(session.playerA(), currency.id()).signum() > 0
                    || session.money(session.playerB(), currency.id()).signum() > 0)) {
                return true;
            }
        }
        return false;
    }

    private void open(TradeSession session, Player player, String partnerName) {
        SchedulerUtils.runForPlayer(plugin, player, () -> {
            if (!session.isOpen()) {
                return;
            }
            var inventory = TradeMenus.create(session, player.getUniqueId(), partnerName);
            session.attachView(player.getUniqueId(), inventory);
            TradeMenus.render(session, player.getUniqueId());
            player.openInventory(inventory);
        });
    }

    private void checkDistance(TradeSession session) {
        Player left = Bukkit.getPlayer(session.playerA());
        Player right = Bukkit.getPlayer(session.playerB());
        if (left == null || right == null) {
            if (left != null) {
                end(left, CancelReason.QUIT, true);
            } else if (right != null) {
                end(right, CancelReason.QUIT, true);
            } else {
                abandon(session);
            }
            return;
        }
        SchedulerUtils.runForPlayer(plugin, left, () -> {
            Location leftLocation = left.getLocation().clone();
            session.remember(left.getUniqueId(), leftLocation);
            SchedulerUtils.runForPlayer(plugin, right, () -> {
                if (!session.isOpen()) {
                    return;
                }
                Location rightLocation = right.getLocation();
                session.remember(right.getUniqueId(), rightLocation);
                if (leftLocation.getWorld() == null || rightLocation.getWorld() == null
                        || !leftLocation.getWorld().equals(rightLocation.getWorld())) {
                    end(left, CancelReason.WORLD, false);
                    return;
                }
                if (tooFar(leftLocation, rightLocation, session.rules().maxDistance())) {
                    end(left, CancelReason.DISTANCE, false);
                }
            });
        });
    }

    private void expire(UUID requestId) {
        Request request = null;
        synchronized (lock) {
            for (Request candidate : incoming.values()) {
                if (candidate.id.equals(requestId)) {
                    request = candidate;
                    break;
                }
            }
            if (request == null) {
                return;
            }
            incoming.remove(request.target, request);
            outgoing.remove(request.sender, request.target);
        }
        Settings settings = plugin.getConfigManager().settings();
        Player sender = Bukkit.getPlayer(request.sender);
        Player target = Bukkit.getPlayer(request.target);
        if (sender != null) {
            tell(sender, settings, "request-expired", Map.of());
        }
        if (target != null) {
            tell(target, settings, "request-expired", Map.of());
        }
    }

    private void dropRequest(UUID sender) {
        UUID target = outgoing.remove(sender);
        if (target == null) {
            return;
        }
        Request request = incoming.get(target);
        if (request != null && request.sender.equals(sender)) {
            incoming.remove(target);
            cancelTask(request.task);
        }
    }

    private void untrack(TradeSession session) {
        synchronized (lock) {
            byPlayer.remove(session.playerA(), session);
            byPlayer.remove(session.playerB(), session);
            byId.remove(session.id(), session);
        }
    }

    private boolean allowed(Player subject, Player notify, Settings settings) {
        if (subject.getWorld() != null && settings.worldBlocked(subject.getWorld().getName())) {
            tell(notify, settings, "request-blocked-world", Map.of());
            return false;
        }
        if (settings.gamemodeBlocked(subject.getGameMode().name())) {
            tell(notify, settings, "request-blocked-gamemode", Map.of());
            return false;
        }
        return true;
    }

    private void tellBoth(TradeSession session, String leftPath, String rightPath) {
        Player left = Bukkit.getPlayer(session.playerA());
        Player right = Bukkit.getPlayer(session.playerB());
        Map<String, String> distance = Map.of("distance", distanceText(session.rules().maxDistance()));
        if (left != null) {
            tell(left, session.rules(), leftPath, distance);
        }
        if (right != null) {
            tell(right, session.rules(), rightPath, distance);
        }
    }

    private void closeBoth(TradeSession session) {
        Player left = Bukkit.getPlayer(session.playerA());
        Player right = Bukkit.getPlayer(session.playerB());
        if (left != null) {
            closeView(left, session);
        }
        if (right != null) {
            closeView(right, session);
        }
    }

    private void closeView(Player player, TradeSession session) {
        session.suppressClose(player.getUniqueId());
        SchedulerUtils.runForPlayer(plugin, player, () -> {
            var top = player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof TradeMenus.TradeHolder holder && holder.sessionId().equals(session.id())) {
                player.closeInventory();
            } else if (top.getHolder() instanceof TradeMenus.PreviewHolder holder && holder.sessionId().equals(session.id())) {
                player.closeInventory();
            }
        });
    }

    private void tell(Player player, Settings settings, String path, Map<String, String> placeholders) {
        if (player == null || settings == null) {
            return;
        }
        String message = settings.prefixed(path);
        if (message.isBlank()) {
            return;
        }
        Runnable send = () -> MessageService.send(player, message, placeholders);
        if (!plugin.isEnabled()) {
            send.run();
            return;
        }
        SchedulerUtils.runForPlayer(plugin, player, send);
    }

    private void play(Player player, Settings settings, String name) {
        if (player == null || settings == null) {
            return;
        }
        SoundSpec spec = settings.sound(name);
        if (spec == null || !spec.enabled() || spec.sound() == null) {
            return;
        }
        SchedulerUtils.runForPlayer(plugin, player, () -> {
            try {
                Sound sound = Sound.valueOf(spec.sound().trim().toUpperCase(java.util.Locale.ROOT));
                player.playSound(player.getLocation(), sound, spec.volume(), spec.pitch());
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    private static boolean tooFar(Location left, Location right, double maxDistance) {
        if (left == null || right == null || left.getWorld() == null || right.getWorld() == null) {
            return true;
        }
        if (!left.getWorld().equals(right.getWorld())) {
            return true;
        }
        double max = Math.max(0D, maxDistance);
        return left.distanceSquared(right) > max * max;
    }

    private static String distanceText(double distance) {
        if (distance == Math.rint(distance)) {
            return Long.toString((long) distance);
        }
        return Double.toString(distance);
    }

    private static void cancelTask(ScheduledTask task) {
        if (task != null) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static final class Request {
        private final UUID id;
        private final UUID sender;
        private final UUID target;
        private final int timeoutSeconds;
        private final double maxDistance;
        private ScheduledTask task;

        private Request(UUID id, UUID sender, UUID target, int timeoutSeconds, double maxDistance) {
            this.id = id;
            this.sender = sender;
            this.target = target;
            this.timeoutSeconds = timeoutSeconds;
            this.maxDistance = maxDistance;
        }
    }
}
