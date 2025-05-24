# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a PhpStorm plugin that provides IntelliSense support for PHP MVC frameworks. It enables type inference and auto-completion for variables passed from controllers to views using `$this->setVar('key', $value)` patterns.

## Common Development Commands

### Build & Development
```bash
./gradlew build                    # Build the plugin
./gradlew runIde                   # Run PhpStorm with plugin for testing
./gradlew buildPlugin              # Create distributable plugin zip
./gradlew verifyPlugin             # Verify plugin structure and compatibility
```

### PHP Testing Environment
```bash
cd php/
composer install                  # Install PHPStan and dependencies
vendor/bin/phpstan analyse        # Run static analysis
php index.php                     # Run test PHP application
```

## Architecture

### Plugin Components
The plugin consists of 4 main extension points registered in `plugin.xml`:

1. **ViewTypeProvider** - Provides type inference for variables in view files by analyzing corresponding controller `setVar()` calls
2. **ViewPredefinedVariableProvider** - Offers auto-completion for available variables in view templates
3. **SetVarUsageTypeProvider** - Marks `setVar()` calls as WRITE usage type for IDE features
4. **ViewReferenceContributor** - Enables navigation between controller variable assignments and view usage

### File Naming Conventions
The plugin uses convention-based mapping between controllers and views:

- **Controller**: `/modules/Cli/Controller/Debug/TestController.php` with `indexAction()` method
- **View**: `/modules/Cli/views/debug/test/index.php`
- **Variable Flow**: `$this->setVar('varName', $value)` in controller → `$varName` auto-completion in view

### Core Logic
The `ControllerFile` model class handles the mapping logic:
- Converts view paths to controller paths using naming conventions
- Extracts method references from controller actions
- Supports nested module structures

### Development Environment
- **Target**: PhpStorm 2025.1 with PHP plugin
- **Java Version**: 21
- **Build System**: Gradle with IntelliJ Platform Plugin 2.5.0
- **Kotlin Support**: Enabled for mixed Java/Kotlin development

### Test Structure
The `php/` directory contains a sample MVC application for testing:
- Controllers extend base `Controller` class with `setVar()` method
- Views use variables passed from controllers
- PHPStan integration with custom rules for variable type analysis

### Logging
Custom `Log` utility provides detailed debugging with caller context including thread ID, class name, method, and line number.