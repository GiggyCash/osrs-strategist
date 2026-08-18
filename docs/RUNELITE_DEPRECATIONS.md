# RuneLite deprecation audit

The remaining compiler warnings use RuneLite's deprecated `Varbits` constants
for account type, achievement diary completion, and Combat Achievement tiers.

The RuneLite API version used by this project does not expose verified,
one-for-one `gameval.VarbitID` replacements for those complete sets. Similar
names are not assumed to have identical semantics. These readers therefore
remain unchanged and fail closed where their state is unavailable. Migrate an
individual reader only after RuneLite publishes a current replacement with
matching meaning, then add loading and account-switch regression coverage.
