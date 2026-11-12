# DonutPay Tracker (Fabric, Minecraft 1.21.11)

Client-side, passive payment tracker for DonutSMP, made for **DILLYY_MC**.

## Features
- Detects real `"<name> paid you $<amount>"` server messages (K / M / B / T suffixes supported)
- Live toast (3 styles x 4 animations), sender name + formatted and exact amount
- Scrolling payment-history HUD + session panel (session total, best payment, top payer)
- **Big-payment celebration at $1,000,000 and up** (threshold adjustable): pulsing title, sparkles, gold glow, special sound
- Stats screen: lifetime total, largest payment, top payers, scrollable history
- Settings GUI (sounds, volume, pitch, style, animation, HUD corner, threshold...)
- Stats saved to `.minecraft/config/donutpay/stats.json`, settings to `config.json`

## Controls
- `K` settings, `J` stats (rebindable), or `/donutpay`, `/donutpay stats`, `/donutpay resetsession`

## Build
Requires JDK 21 and Gradle 9.2+ (internet needed for the first build).

    gradle build

Jar: `build/libs/donutpay-tracker-1.0.1.jar` (not the `-sources` jar).
Install: put it plus **Fabric API 0.141.3+1.21.11** in `.minecraft/mods` with Fabric Loader >= 0.18.1.

No Gradle? Push this folder to GitHub: the included workflow builds the jar (Actions -> artifact).

## Safety / verification
- Only listens to **system messages from the server** (`ClientReceiveMessageEvents.GAME`), never signed player chat.
- The pattern is anchored to the start of the line, so chat like `<Bob> X paid you $1B` is ignored.
- Counts only when logged in as the configured owner and (by default) connected to a `donutsmp` server.
- "Preview" buttons are labelled `[PREVIEW]` and are never saved or counted.
- The mod never sends chat/commands/payments, reads no tokens, and makes no network connections.

## If your payments don't show up
DonutSMP's exact wording may differ slightly from the video. Edit `PAID_YOU` in `DonutPayClient.java`
(copy the exact line from your chat) and rebuild. Note: if the server ever relays normal chat as system
messages, a player could imitate a payment line, so treat the tracker as a convenience, not proof of funds.
