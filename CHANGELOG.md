# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). Add every user-visible change under `Unreleased`; the release
workflow publishes the matching section as the GitHub Release notes.

## [Unreleased]

### Changed
- The app is now named LastWhen and has a new launcher icon (a history arrow around clock hands).
- The About link now points to the GitHub repository.
- The application id is now `io.github.desvelao.lastwhen`, so it installs as a new app: uninstall the old build and restore from a backup.

### Added
- Backups also save your sort, theme, dynamic colour and daily digest settings (not the app lock), and restore them.
- About dialog (opened from the version in Settings) with the app description and a link to open or copy.
- Spanish, German, French and Italian translations, with a Language setting (default: system language, English if unavailable).
- Add a new category straight from the event form.
- Hours and Days age filters on Home.
- Events with icon, colour and category; days-since counter on every card.
- One-tap Done with Undo, and a log sheet (date, time, Yesterday, note) on long-press.
- Event detail with history and gaps, statistics, rhythm progress; edit and remove entries.
- Target rhythm with Overdue / Due soon / Later sections and status chips.
- Categories screen, search over names and notes, age and category filters, sorting.
- Archive and restore events.
- Settings: theme, dynamic colour, JSON backup/restore, CSV export, daily digest, app lock.
- Home-screen widgets: single event with Done, and overdue list.

### Fixed
- With the app lock on, the daily digest shows only the overdue count on the device lock screen, not event names.
- The app lock now locks again after the app has been in the background for 30 seconds.
- Widgets now refresh hourly and when the date or time changes, so elapsed labels and Overdue stay current.
- The daily digest stays at the chosen local time across daylight-saving, time zone and clock changes.
- Restoring a damaged backup (unknown category, duplicate ids) shows a clear message instead of failing; backups are read consistently; CSV export also guards cells starting with a tab or line break.
- Event names sort with the language's alphabet order (accented letters included) and ties keep a stable order.
- The progress bar and the Overdue/Due soon status now agree across daylight-saving changes.
- Colour swatches now have a full 48dp touch target, and the selected check mark picks black or white for contrast.


## [0.1.0] - 2026-11-15

First release.

### Added
- Project scaffolded from the `android-compose` template.
