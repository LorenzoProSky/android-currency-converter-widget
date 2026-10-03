# Technical documentation

How this widget is built, how it works end to end, and why it's built the way it is.

## How it works, end to end

### The widget isn't your UI

The launcher app renders the widget, not your process. Your code never draws a pixel directly — it builds a `RemoteViews` object (a description of what should appear) and hands it to `AppWidgetManager`, which ships it across to the launcher. This shapes almost everything else below: no direct click listeners, no animations, no layout types outside a fixed allow-list, and no state kept in memory between updates — everything has to be explicitly persisted and explicitly re-sent.

### Lifecycle

`CurrencyWidgetProvider` is a `BroadcastReceiver` under the hood, and the single hub for everything:

- `onUpdate()` — fires when a widget is placed. Renders instantly from whatever's cached, then checks if rates are stale.
- `onReceive()` — fires on every tap. Reads a custom action + widget ID (+ a `key` extra for which digit was pressed) off the `Intent`, dispatches to a handler, wrapped in a top-level `try/catch` so a bad action can never take down the whole broadcast.
- `onDeleted()` — clears that widget's saved state when it's removed, so orphaned prefs don't accumulate.

### State

`WidgetState` persists each widget's amount, source, and target in `SharedPreferences`, keyed by `appWidgetId`. Every pinned instance is fully independent — two widgets on the same home screen never see each other's state. The amount is stored as the literal typed string, which sidesteps an entire class of parsing/rounding edge cases.

### Rendering

`computeContent()` is the single source of truth — reads current state and cached rates, converts, formats both strings (or returns `"NO DATA"` if nothing's cached yet), and every render path pulls from it so none of them can drift out of sync with each other.

Two tiers of render exist on top of that:
- **Full** (`updateWidget`) — sends every view plus attaches every `PendingIntent`. Used exactly once per widget, at placement.
- **Scoped partial** (`updateAmountAndResult`, `updateSourceAndResult`, `updateTargetAndResult`, `updateAfterSwap`, `updateResultOnly`) — each touches only the 1–3 views that specific action could possibly have changed, sent via `partiallyUpdateAppWidget`. This is what keeps, say, typing a digit from visibly disturbing the currency pills.

### Interaction

Each button is wired to a `PendingIntent` — a sealed capsule telling the launcher "broadcast this exact `Intent` back to my app on tap." That's the only bridge available, since the launcher is a separate process and can't call your Kotlin directly. All 13 interactive views share four action strings (cycle source, cycle target, swap, keypad), disambiguated by the `appWidgetId` and, for the keypad, a `key` extra — with each `PendingIntent`'s `requestCode` built from all three, since Android compares intents for sameness in a way that ignores extras by default.

### Rates

`RatesRepository` caches fetched rates as a JSON blob (EUR-pivoted, so one fetch covers every pair among the six currencies) plus a timestamp in `SharedPreferences`, mirrored into an in-memory field to skip re-parsing that JSON on every tap. `convert()` pivots any pair through EUR: divide into EUR, multiply out. `refreshCacheAsync()` checks staleness (3 hours) and, if needed, enqueues `RatesRefreshWorker` through `WorkManager` with `ExistingWorkPolicy.KEEP` — so a burst of taps can never spawn duplicate network calls — constrained to only run when there's a real connection. Critically, the network only ever runs inside that worker, never on the tap path itself, which is what makes every interaction instant regardless of connectivity.

### Icon

An adaptive icon: two flat vector layers (a solid background, a foreground glyph) that the OS composites and masks into whatever shape a given launcher uses. The glyph itself was traced pixel-for-pixel from a reference image — threshold, contour-detect, simplify, normalize into the icon's coordinate space — rather than hand-approximated, with a left/right symmetry pass applied afterward by mirroring one verified-correct half.

## Key decisions and trade-offs

- **Tap-to-cycle currencies, not a picker.** `RemoteViews` has no dropdown/spinner. One tap per change, at the cost of up to 5 taps to reach the furthest currency in the list.
- **Calculator-style amount entry, not a cursor.** Digits type in normally, a dedicated dot key handles the decimal, and there's no mid-string editing — no invalid states are possible, since the input can never be malformed.
- **Always render from cache, refresh in the background.** A tap never waits on the network. The cost is a rate that can be up to 3 hours stale before self-correcting — an explicit trade of perfect freshness for guaranteed responsiveness.
- **Clear-all instead of delete digit.** `RemoteViews` has no long-press callback, and a long-press on a widget's surface is reserved by the OS for its own move/resize/remove overlay. A dedicated Clear button was the best substitute, preferred over a single digit deletion action.
- **Daily reference rates, not live market data.** Frankfurter publishes ECB rates once a day. Free and keyless, which mattered more here than intraday precision.

## Project structure

| File | Role |
|---|---|
| `CurrencyWidgetProvider.kt` | Widget lifecycle, tap dispatch, all render functions |
| `WidgetState.kt` | Per-widget persisted state (amount, source, target) |
| `RatesRepository.kt` | Rate caching, EUR-pivot conversion, staleness checks |
| `RatesRefreshWorker.kt` | Background rate fetch via `WorkManager` |
| `CurrencyConverterApplication.kt` | On-demand `WorkManager` initialization |
| `WidgetActions.kt` | Shared action/extra constants, digit-key ID map |
| `CurrencyConstants.kt` | Currency list, API endpoint |
| `res/layout/currency_widget.xml` | The widget's UI |
| `res/drawable/ic_launcher_*.xml` | Adaptive icon layers |