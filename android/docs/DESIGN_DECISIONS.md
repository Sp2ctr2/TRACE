# Design decisions

## Source

TRACE site and its repository `index.html` are the visual reference. The dark Android prototypes supply only timeline, sheet and easy-mode information architecture. The app does not reproduce a phone frame, fake status bar, camera hole or home indicator.

## Native translation

Warm paper and near-white surfaces; coral brand with a deeper accessible CTA; ink-led type and thin dividers. The source website's small desktop mockup text is not copied at physical size. Native content uses readable sp, scrollable space, minimum 48dp controls and safe/IME insets.

Five banking tabs remain the normal shell. TRACE only takes screen-level priority when a specific transfer needs attention. There is no ambient scanning animation, glow, emoji, AI avatar or numerical risk gauge.

## Deliberate differences

The interactive web scenario's browser-only receipt lifetime becomes Room persistence. The web page's small repeated English demo labels become a concise Korean disclosure at authentication, result and account boundaries. HOLD is implemented below the UI, not just as a missing button. WARN is a separate review flow. Official route lookup creates a new transaction rather than reusing an approval.

## No false claims

No real bank, police, credit bureau or model accuracy is implied. Native ECDSA transaction binding is distinguished from remote hardware attestation. A rule analyzer is labeled as a rule analyzer. Input patterns are not treated as actual call interception or actual malicious-link access. Financial totals are computed from coherent seeded data, not copied as independent dashboard numbers.
