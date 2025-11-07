# UID Component

Unified interface for unique id generation.

## Features

### UUID (RFC9562)

- **v0**: Null UUID
- **v1**: Time-based with MAC address
- **v3**: Name-based with MD5
- **v4**: Random (cryptographically secure)
- **v5**: Name-based with SHA1
- **v6**: Time-based, lexically sortable
- **v7**: Unix time-based, lexically sortable
- **v8**: Custom UUID with user data
- **SQUUID**: Sequential UUID (non-standard)

### Flake

- 192-bit time-ordered identifiers
- Nanosecond precision
- 128-bit entropy
- URL-safe string encoding

## Usage

```clojure
(require '[com.vadelabs.uid.interface :as uid])

;; UUID generation
(uid/v4)     ; Random UUID
(uid/v7)     ; Time-ordered UUID

;; Flake generation
(uid/flake)      ; Returns Flake object
(uid/snowflake)  ; Returns URL-safe string
```

## Structure

```
uid/
├── interface.clj          # Unified public API
├── uuid/                  # RFC9562 UUID implementation
│   ├── interface.clj      # UUID public API
│   ├── core.clj          # Core UUID logic
│   ├── bitmop.clj        # Bit manipulation
│   ├── clock.clj         # Monotonic clock
│   └── ...
└── flake/                # Flake implementation
    ├── interface.clj      # Flake public API
    ├── core.clj          # Core Flake logic
    ├── impl.clj          # Implementation details
    └── nanoclock.clj     # Nanosecond clock
```

## Acknowledgements

This library was inspired by and references implementations from:

- **[μ/log (mulog)](https://github.com/BrunoBonacci/mulog)** by Bruno Bonacci - Flake implementation reference
  - The Flake implementation in this library is based on the design from mulog's [flakes.clj](https://github.com/BrunoBonacci/mulog/blob/master/mulog-core/src/com/brunobonacci/mulog/flakes.clj)
  - Licensed under Apache License 2.0

- **[clj-uuid](https://github.com/danlentz/clj-uuid)** by Dan Lentz - UUID implementation reference
  - The UUID implementation follows patterns and approaches from clj-uuid
  - Licensed under Eclipse Public License 1.0

We are grateful to these projects and their maintainers for their excellent work and open source contributions.

## License

Copyright © 2025 Vade Labs

Distributed under the MIT License. See LICENSE for details.
