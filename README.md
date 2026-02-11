# Koin-Detekt

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Detekt extension library with rules for Koin 4.x to enforce best practices and catch common anti-patterns via static analysis.

## 🚧 Work in Progress

This project is currently under development. The goal is to provide a comprehensive set of static analysis rules for Koin dependency injection framework.

## Planned Features

- **Service Locator Anti-patterns Detection** - Prevent direct usage of get(), inject(), and KoinComponent outside module definitions
- **Module DSL Best Practices** - Enforce proper module organization and configuration
- **Scope Management** - Detect scope lifecycle issues and memory leaks
- **Zero runtime overhead** - Pure syntactic analysis via Kotlin PSI
- **Highly configurable** - Customize rules via detekt config

## Requirements

- Kotlin 2.0+
- Detekt 1.23.8+
- Targets Koin 4.x (all platforms: Core, Android, Compose, Ktor)

## Development Status

Project structure initialized. Rules implementation in progress.

## License

```
Copyright 2026 Kirill Rozov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please open an issue or pull request.
