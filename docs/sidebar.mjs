const root = "rextrade";

export const rextrade = {
  label: "RexTrade",
  items: [
    { label: "Overview", slug: root },
    {
      label: "Getting started",
      items: [
        { label: "Installation", slug: `${root}/getting-started/installation` },
        { label: "Requirements", slug: `${root}/getting-started/requirements` },
      ],
    },
    {
      label: "Configuration",
      items: [
        { label: "Settings", slug: `${root}/configuration/overview` },
        { label: "Currencies", slug: `${root}/configuration/currencies` },
      ],
    },
    {
      label: "Guides",
      items: [
        { label: "Trading", slug: `${root}/guides/trading` },
      ],
    },
    {
      label: "Reference",
      items: [
        { label: "Commands", slug: `${root}/reference/commands` },
        { label: "Permissions", slug: `${root}/reference/permissions` },
      ],
    },
    { label: "FAQ", slug: `${root}/faq` },
  ],
};
