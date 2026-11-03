# NextDNS on the Home base tile

SpiderVision never changes DNS. The TV's own Private DNS already points at NextDNS
(`<profile>.dns.nextdns.io`); SpiderVision reads that hostname to learn the profile ID and, once
linked, shows that profile's last-24-hour totals.

## Link / unlink (on the PC)

```powershell
cd C:\Users\mahin\Desktop\orrery-tv-launcher\tools
.\link-nextdns.ps1            # asks for the API key (hidden input)
.\link-nextdns.ps1 -Unlink    # removes it from the TV
```

The API key is on my.nextdns.io → Account → API. Paste it only into the script prompt: not into
chat, the repo, a file or a screenshot.

## How the key is protected (owner-approved option: "key stored on the TV")

| Concern | What SpiderVision does |
| --- | --- |
| In the APK or repo | Never. It only arrives at runtime from the PC. |
| Who can deliver it | `ProvisionReceiver` requires the sender to hold `WRITE_SECURE_SETTINGS`: the ADB shell has it, ordinary TV apps can't get it. |
| At rest | AES-GCM with a key held in Android Keystore, in SpiderVision's private storage. `allowBackup=false`. |
| In logs or on screen | Never logged, displayed or returned; the receiver only answers `linked` / `unlinked` / `rejected`. Verified: a test key did not appear in `logcat`. |
| Caching | Only totals (queries, blocked, hourly counts, time) are cached. Blocked domain names stay in memory and are shown only in the Home base sheet. |

**Trade-off:** the NextDNS API key has full account access. Keystore keeps it from other apps
and from copied storage, but a fully compromised TV (root) could still recover it. The stronger
option is a small read-only proxy that holds the key instead; it can replace this later without UI
changes, because the tile only depends on `NextDnsClient.fetch`.

## What the tile shows

| State | Badge | Content |
| --- | --- | --- |
| Not linked | NOT LINKED | "NextDNS stats not connected", empty chart frame |
| Loading, nothing cached | LOADING | "Loading NextDNS stats…" |
| Live | LIVE · 24 H | Queries, blocked %, blocked count, 12 hourly bars, "Updated h:mm" |
| Network failure | OFFLINE | last good numbers + "Couldn't reach NextDNS · showing h:mm numbers" |
| Bad key / wrong profile | CHECK KEY | "NextDNS rejected the saved key" (no retry until next visit or Refresh) |
| Private DNS not NextDNS | — | "The TV's Private DNS isn't set to NextDNS" |

Never fictional: with no data the chart is an empty frame.

## Refresh policy

Only while SpiderVision is on screen: on open, then every 10 minutes. Failures back off
2 → 4 → … → 60 minutes; an auth failure waits for the next visit or "Refresh now".
5 s connect / 7 s read timeouts. No background service, no polling when the TV is elsewhere.

## Endpoints used

- `GET /profiles/{id}/analytics/status?from=-24h` — totals by status
- `GET /profiles/{id}/analytics/status;series?from=-12h&interval=3600` — hourly bars
- `GET /profiles/{id}/analytics/domains?status=blocked&from=-24h&limit=4` — top blocked

## Acceptance still open

- Link with the real key and compare the tile's 24-hour numbers with NextDNS web Analytics for
  the same window (owner to do: the key must not pass through anyone else).
- Confirm the hourly series parses (the series request is untested until a real key is linked;
  if it fails, totals still show and the chart stays an empty frame).
