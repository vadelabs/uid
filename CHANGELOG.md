# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to date-based versioning (YYYY.MM.DD-N).

## [Unreleased]

## [2025.11.13-29] - 2025-11-13

### Added
- Comprehensive benchmark suite for UUID and Flake performance testing using Criterium
- Benchmark results verify generation, encoding, parsing, and comparison performance
- Detailed performance benchmarks with scale testing (1K, 10K, 100K operations)
- Comparison benchmarks against java.util.UUID

### Performance
- Add type hints to `encode->string` hot path for improved encoding performance
- Add type hints to `decode<-string` hot path for faster parsing

### Changed
- Consolidated API: removed duplicate `flake-bytes` function, use `as-bytes` instead
- Refactored UUID v1/v6: extract duplicate LSB logic into `make-lsb` function
- Simplified UUID comparison functions with more idiomatic patterns
- Removed redundant convenience wrapper functions from UUID implementation
- Removed redundant `some?` checks in regex matching

### Documentation
- Add comprehensive namespace documentation for Flake implementation
- Add comprehensive v8 custom UUID documentation with examples
- Add visual bit layout diagrams for timestamp extraction
- Add comprehensive thread-safety documentation for clock implementation
- Document clock sequence purpose and lifecycle
- Add comprehensive docstrings to UUIDRfc9562 protocol methods
- Extract magic numbers to named constants with clear explanations

### Fixed
- Correct byte range validation in `uuid-vec?` predicate
- Adjust flake-time-test for nanoclock precision handling

### Refactoring
- Consolidate fine-grained accessors into structured maps for cleaner API
- Extract magic numbers to named constants in Flake implementation
- Improve code formatting and indentation consistency
- Use destructuring in tests instead of convenience functions

## [2025.11.07-15] - 2025-11-07

### Added
- UUID (RFC9562) implementation with support for v0, v1, v3, v4, v5, v6, v7, v8, and SQUUID
- Flake implementation (192-bit time-ordered identifiers)
- Comprehensive test suite with 77 tests and 4816 assertions
- Unified public API via `com.vadelabs.uid.interface`
- Automated versioning and release workflow
- Code coverage reporting
