# Notes for coding agents

Read [CONTRIBUTING.md](CONTRIBUTING.md) before making changes.

## Strings

When you add a string to `app/src/main/res/values/strings.xml` or
`app/src/main/res/values/error.xml`, or change its meaning or placeholders, add
or update the entry with the same name in the matching file in
`app/src/main/res/values-qq/`. If you remove a string, remove its qq entry.
Strings with `translatable="false"` don't need one. Translators see the qq
text as the only context for the string. See "Document every new string for
translators" in CONTRIBUTING.md for what to write.
