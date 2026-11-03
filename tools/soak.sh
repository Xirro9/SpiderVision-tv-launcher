#!/usr/bin/env bash
# 20-minute D-pad soak for SpiderVision. Arrow keys only (never OK/Menu/Back), so it can't open
# apps or change settings. Samples memory every 2 minutes and records crashes/ANRs.
# Usage: tools/soak.sh <adb.exe> <serial> <out-file> [minutes]
set -u
export MSYS_NO_PATHCONV=1
A="$1"; T="-s $2"; OUT="$3"; MIN="${4:-20}"
keys=(DPAD_UP DPAD_DOWN DPAD_LEFT DPAD_RIGHT)
"$A" $T logcat -c
"$A" $T shell am start -n com.spidervision.tv/.MainActivity >/dev/null 2>&1
sleep 5
pid0=$("$A" $T shell pidof com.spidervision.tv | tr -d '\r')
echo "start $(date +%T) pid=$pid0" > "$OUT"
end=$(( $(date +%s) + MIN * 60 )); next=0; presses=0
while [ "$(date +%s)" -lt "$end" ]; do
  if [ "$(date +%s)" -ge "$next" ]; then
    top=$("$A" $T shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o 'u0 [^ ]*')
    pss=$("$A" $T shell dumpsys meminfo com.spidervision.tv | grep 'TOTAL PSS' | awk '{print $3}')
    echo "$(date +%T) presses=$presses pss_kb=$pss pid=$("$A" $T shell pidof com.spidervision.tv | tr -d '\r') top=$top" >> "$OUT"
    case "$top" in *spidervision*) ;; *) echo "STOP: SpiderVision left the foreground" >> "$OUT"; break;; esac
    next=$(( $(date +%s) + 120 ))
  fi
  "$A" $T shell input keyevent KEYCODE_${keys[$((RANDOM % 4))]}
  presses=$((presses + 1))
  sleep 0.3
done
echo "end $(date +%T) presses=$presses" >> "$OUT"
echo "crash buffer lines for spidervision: $("$A" $T logcat -d -b crash | grep -c spidervision)" >> "$OUT"
echo "ANR lines for spidervision: $("$A" $T logcat -d | grep -E 'ANR in|Application Not Responding' | grep -c spidervision)" >> "$OUT"
"$A" $T shell dumpsys gfxinfo com.spidervision.tv | grep -E "Total frames rendered|Janky frames:|50th percentile|90th percentile|99th percentile" >> "$OUT"
