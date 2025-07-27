# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System and Common Commands

Freeplane uses Gradle as its primary build system. **Always use `gradle` command** (not `gradlew` or `maven`).

### Commands for Claude Code
- `gradle :freeplane:compileJava` - Compile freeplane subproject (run before committing)
- `gradle :subproject:compileJava` - Compile specific subproject if modified
- `gradle :freeplane:test` - Run tests for validation
- `gradle format_translation` - **Always run after changing language resource files**

### Distribution Commands
- `gradle dist` - Create distribution packages
- `gradle mac.dist` - Create macOS distribution
- `gradle win.dist` - Create Windows distribution
- `gradle linux-packages` - Create Linux packages

## Project Architecture

Freeplane is a Java-based mind mapping application built with OSGi architecture and Swing UI.

### Core Architecture
- **OSGi Framework**: Uses Knopflerfish OSGi framework for modular plugin system
- **Multi-module Gradle project**: 17+ submodules with clear separation of concerns
- **Java 8 compatibility**: Targets Java 8 for broad compatibility - avoid Java 9+ features
- **Plugin system**: Extensible through OSGi plugins for features like LaTeX, scripting, SVG support

### Key Modules
- `freeplane` - Core application (org.freeplane.core bundle)
- `freeplane_api` - Public API for plugin development
- `freeplane_framework` - OSGi framework and launcher
- `freeplane_plugin_*` - Feature plugins (script, latex, markdown, etc.)
- `freeplane_mac` - macOS-specific functionality
- `JOrtho_0.4_freeplane` - Spell checking component

### Source Structure
- `src/main/java` - Production code
- `src/test/java` - Unit tests
- `src/editor/resources` - Editor-specific resources (images, translations, CSS)
- `src/viewer/resources` - Viewer-specific resources
- `src/external/resources` - External resources (templates, XSLT transformations)

### Build Output
- `BIN/` - Global build output directory containing the complete application
- Plugin JARs are copied to `BIN/plugins/{plugin.id}/lib/`
- Core application JARs go to `BIN/core/org.freeplane.core/lib/`

## Development Guidelines

### Code Standards (from .cursor/rules)
- **No comments or Javadoc** in final code - use self-documenting naming
- **Incremental refactoring** - small testable steps with commits after each step
- **Extract only what's used** - avoid speculative generality
- **Single responsibility** - each extracted class should have clear purpose
- **Remove unused imports** - Clean up imports after coding changes to keep code tidy

### OSGi Bundle Configuration
- Plugin bundles require `Bundle-Activator` and `Require-Bundle: org.freeplane.core`
- Bundle dependencies are externalized via `Bundle-ClassPath`
- Core module exports packages for plugin consumption

### Translation System
- Translation files in `src/*/resources/translations/Resources_*.properties`
- Use `check_translation` and `format_translation` gradle tasks
- Support for 25+ languages including RTL languages

#### Translation Key Conventions
- **OptionPanel prefix**: UI preference keys use `OptionPanel.{key}={value}` format
- **Separator titles**: Use `OptionPanel.separator.{name}={title}` for section headers
- **Choice values**: Combo box options use same key as preference choice value
- **Alphabetical sorting**: Properties are sorted alphabetically ignoring case (intentional)
- **Always run format_translation**: After any translation file changes to maintain sorting

### Testing
- JUnit 4.13.2 with Hamcrest and Mockito
- AssertJ for fluent assertions
- Use `gradle test` or `gradle :module:test` for specific modules
- Test logging can be enabled with `-PTestLoggingFull`

## Plugin Development
- Extend from OSGi bundle structure
- Use `freeplane_api` for public interfaces
- Follow naming convention: `org.freeplane.plugin.{name}`
- Bundle activator required for OSGi lifecycle management

## Code Structure Learnings

### Mouse Event Handling Architecture
- **NodeSelector** - handles node selection timing and behavior
- **NodeFolder** - handles node folding timing and behavior (independent from selection)
- **DefaultNodeMouseMotionListener** - coordinates mouse events between NodeSelector and NodeFolder
- Events are routed based on mouse regions (folding vs selection areas)

### Filter and Map Context Patterns
- **Always get Filter from MapView**: Use `map.getFilter()` from the MapView context
- **After map.select()**: Can use `controller.getSelection().getFilter()` - they're equivalent
- **Follow existing patterns**: Most code operates on selected node/map view with consistent filter usage
- **Method signatures matter**: `setFolded(node, boolean, filter)` vs `unfoldAndScroll(node, filter)` vs `toggleFoldedAndScroll(node)` (handles filter internally)

### Property Management and Migration
- **ApplicationResourceController.isPropertySetByUser()**: Checks if property exists in user's props (reliable way to detect user customization)
- **Static migration blocks**: Use for one-time property updates on startup
- **Self-documenting method names**: Replace explanatory comments with well-named methods
- **Default properties**: Add to `freeplane.properties` for system defaults

### Mouse Event Coordination
- **Separate timing from behavior**: Shared timing (`immediate`/`delayed`) used by both selection and folding
- **Regional event routing**: `isInFoldingControl()` vs `isInside()` determines which handler processes the event
- **Legacy compatibility**: Support both old `selection_method` and new granular configuration

### OSGi and Complex Architecture
- Freeplane is complex software without comprehensive tests
- Follow existing patterns rather than reinventing
- Check method signatures carefully (compilation catches parameter mismatches)
- Use existing controller methods rather than lower-level operations

### Logging System
- **Freeplane uses proprietary logging utilities** (not standard Java logging)
- Standard Java logging configuration in `logging.properties` only affects Swing and other Java log records
- For Freeplane-specific logging, use the proprietary logging utilities directly
- Use `LogUtils.warn()` and similar methods for Freeplane logging (as seen in existing codebase)

## Distribution and Packaging
- Multi-platform support (Windows, macOS, Linux)
- Portable application support via PortableApps format
- Windows installer via Inno Setup
- macOS DMG with codesigning support
- Linux packages for Debian-based systems