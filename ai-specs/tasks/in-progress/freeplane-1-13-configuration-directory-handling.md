# Task: Freeplane 1.13 configuration directory handling
- **Task Identifier:** 2026-02-02-configuration
- **Scope:**
  - Create a documentation-only directory for Freeplane 1.13 at
    `<user-dir>/1.13.x` on first start, where `<user-dir>` is
    `Compat.getApplicationUserDirectoryExcludingVersion()`.
  - Keep all active configuration and user data in `<user-dir>/1.12.x`.
  - Avoid migrating, duplicating, or modifying existing user
    configuration files.
  - Ensure `README.txt` is the only file created in `1.13.x` and is
    created only when missing.
  - Store the README content as a resource at
    `freeplane/src/editor/resources/userReadme_1.13.txt` and write it
    to disk when needed.
  - Keep the README content ASCII-only to avoid encoding issues.
- **Motivation:**
  - Freeplane 1.13 introduces only AI integration, which is opt-in.
  - The configuration format is unchanged from 1.12.x.
  - Users should not experience configuration churn or data risk.
- **Developer Briefing:**
  - Startup currently checks for `auto.properties` in the current
    version directory and copies the previous version if missing.
  - For 1.13, keep the current configuration directory at `1.12.x` and
    create `1.13.x` only to host a README documenting the decision.
  - The base user directory is not always `~/.freeplane`; it can be
    overridden by `org.freeplane.userfpdir` (launcher `userDirectory`,
    or `-U` command-line option).
  - Do not create any files in `1.13.x` other than `README.txt`.
  - Load README content directly from the classpath and write it as-is.
  - The README must be created only when missing and must not overwrite
    user edits.
- **Research:**
  - `FreeplaneGUIStarter` constructs `UserPropertiesUpdater` in its
    constructor and calls `importOldProperties()` before
    `ApplicationResourceController` is initialized.
  - `UserPropertiesUpdater.importOldProperties()` checks
    `ApplicationResourceController.getUserPreferencesFile()` (currently
    `<user-dir>/1.12.x/auto.properties`) and returns early if it exists.
  - If `auto.properties` is missing, `UserPropertiesUpdater` copies the
    previous version directory from `Compat.PREVIOUS_VERSION_DIR_NAME`
    to the current version directory, filtering out logs, backups, and
    compiled scripts.
  - `Compat.getApplicationUserDirectory()` appends
    `Compat.CURRENT_VERSION_DIR` (currently `/1.12.x`) to the base user
    directory, so all preferences read/write flow through that
    directory.
  - `Compat.getApplicationUserDirectoryExcludingVersion()` resolves the
    base user directory from `org.freeplane.userfpdir` (set by launcher
    `userDirectory(...)` or `-U`) or falls back to
    `System.getProperty("user.home") + "/.freeplane"`.
- **Design:**

```plantuml
set separator none
title Freeplane 1.13 startup configuration directory flow
actor User
participant FreeplaneGUIStarter
participant UserPropertiesUpdater
participant UserReadmeWriter
participant ApplicationResourceController
participant Compat
database "User Directory\n<user-dir>" as UserHome

User -> FreeplaneGUIStarter : launch 1.13
FreeplaneGUIStarter -> UserPropertiesUpdater : importOldProperties()
UserPropertiesUpdater -> UserReadmeWriter : ensureReadmeExists()
UserPropertiesUpdater -> Compat : getApplicationUserDirectory()
Compat --> UserPropertiesUpdater : <user-dir>/1.12.x
UserPropertiesUpdater -> ApplicationResourceController : getUserPreferencesFile()
ApplicationResourceController --> UserPropertiesUpdater : <user-dir>/1.12.x/auto.properties
UserPropertiesUpdater -> UserHome : ensure <user-dir>/1.13.x exists
UserPropertiesUpdater -> UserPropertiesUpdater : load classpath resource\nuserReadme_1.13.x.txt
UserPropertiesUpdater -> UserHome : create README.txt if missing
UserPropertiesUpdater -> UserHome : read/write only in 1.12.x
UserPropertiesUpdater --> FreeplaneGUIStarter : status (current/old/not found)
```

The `UserPropertiesUpdater` gains a 1.13-only documentation step that
creates `<user-dir>/1.13.x` and `README.txt` if they do not exist, while
leaving `Compat.CURRENT_VERSION_DIR` pointing to `1.12.x` so all
configuration reads and writes remain unchanged. The README creation is
idempotent and never overwrites existing content. The existing copy
logic remains intact but continues to target `1.12.x`, so no migration
to `1.13.x` occurs.

README content is bundled as
`freeplane/src/editor/resources/userReadme_1.13.txt` and read directly
from the classpath, then written verbatim using ASCII-only text to
avoid encoding issues across platforms.
- **Test specification:**
  - Automated tests:
    - Verify `UserPropertiesUpdater` creates `<user-dir>/1.13.x` and a
      `README.txt` when the directory is missing.
    - Verify `README.txt` is not overwritten when it already exists.
    - Verify `auto.properties` is still read from `1.12.x` after 1.13
      startup.
    - Verify `README.txt` content matches the bundled resource.
  - Manual tests:
    - Start 1.13 with an existing `<user-dir>/1.12.x` configuration
      and confirm settings are preserved.
    - Confirm `<user-dir>/1.13.x/README.txt` matches the provided
      content exactly.
