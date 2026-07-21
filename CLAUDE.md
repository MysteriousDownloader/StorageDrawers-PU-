# CLAUDE.md
<!-- Keep this file a POINTER. Duplicating context content here is how drift starts:
     two files claiming the same fact means one of them is eventually wrong. -->

Storage Drawers — porting jaquadro's bulk-storage mod from MC 1.21.10 to Fabric on MC 26.2.

Build: `./gradlew :fabric:build`
Run:   `./gradlew :fabric:runClient`

## Context system
Every session: read `../context/PROJECT.md` (contains the full protocol), then
`../context/STATE.md`, and follow PROJECT.md § Protocol for everything else.
Paths are relative to this file — the context folder lives one level **above** this
repo, because the git repo is a subfolder of the project root. If it ever moves, this
is the single pointer to update.
