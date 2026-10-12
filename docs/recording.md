# In-game video recorder (dev only)

A dev-only recorder that films the dev client from a shot script and writes an mp4. It is inert unless the client
JVM runs with `-Dbts.rec=true`: the client proxy registers it only then, and in normal play none of its classes load.
Code: `src/main/java/com/nateyburr/beyondthestars/rendertest/Rec*.java` and `FrameRecorder.java`. Example scripts:
`tools/recording/*.shots`.

## What it does

1. It locks the window to the video size (1920x1080 by default), hides the HUD, the hand and the debug screen, and
   turns off view bobbing and vsync.
2. From the main menu it creates a fresh creative superflat world (`run/client/saves/bts-rec-<time>`) on the
   integrated server, the same way the autoshots photo harness does. No server port is used.
3. It runs the script's setup lines, then the scenes in order.
4. While a recording command runs, every rendered frame is read with `glReadPixels` (after the world, the GUI and the
   recorder's own caption are drawn) and piped as raw RGB to an ffmpeg child process. Each scene gets its own
   ffmpeg process and segment file.
5. At the end the segments are joined with ffmpeg's concat demuxer (`-c copy`) into one mp4, and the client closes.

**Real speed.** Frames go out on a fixed wall-clock schedule (30 fps by default). If the game renders slower, the last
frame is repeated; if it renders faster, the extra frames are not sent. So one second of game time is one second of
video, apart from time-lapses. `rec.log` gives the render fps and the number of repeated frames for each scene.

**Cuts.** Only the recording commands put frames in the video: `card`, `hold`, `orbit`, `dolly`, `pan` and
`timelapse`. Everything else happens between recorded spans and is cut out. That includes `goto` (teleport, then wait
for the chunks to arrive and render), GUIs opening, builds and waits.

**The camera** is the player, flying. During `orbit`, `dolly` and `pan` the client places it on every rendered frame
(position and rotation, with nothing left to interpolate), so moves are smooth at any frame rate, not stepped per tick.
The mouse is released, so moving it does not turn the view.

## Running it

```
cd GTNH-Projects/addon-autoshots
JAVA_TOOL_OPTIONS="-Dbts.rec=true -Dbts.rec.script=tools/recording/smoke.shots -Dbts.rec.ffmpeg=C:/path/to/ffmpeg.exe" ./gradlew.bat runClient --no-daemon
```

This buildscript's `runClient` takes no JVM-argument property, but `JAVA_TOOL_OPTIONS` reaches the forked client (as for
the render harness). Do not run it while another gradle build is running in the same worktree. The client closes
itself when the script is done.

Output (default `run/recordings/`, git-ignored with the rest of `run/`):

- `run/recordings/<script>-<yyyyMMdd-HHmmss>.mp4`: the finished video.
- `run/recordings/<script>-<yyyyMMdd-HHmmss>/`: the segments (`NN_<scene>.mp4`, each with an ffmpeg `.log`), a copy
  of the script, `concat.txt`, and `rec.log` (every command, the camera moves and a per-scene summary: video seconds,
  frames rendered and repeated, render fps).

### Properties

| Property | Default | Meaning |
|---|---|---|
| `bts.rec` | off | The switch. Nothing loads without `true`. |
| `bts.rec.script` | (required) | The shot script. A relative path is looked up from the client's working directory (`run/client`) and from the worktree root. |
| `bts.rec.ffmpeg` | `ffmpeg` | Path to ffmpeg (7.x with libx264, and h264_nvenc if you want it). The binary is not in the repo. |
| `bts.rec.out` | `run/recordings` | Output folder. |
| `bts.rec.size` | `1920x1080` | Window and video size, e.g. `1280x720`. On the dev machine (RTX 4090) the game renders 500 to 800 fps at 1080p while recording, far above the 30 needed. |
| `bts.rec.fps` | `30` | Video frame rate. |
| `bts.rec.encoder` | `auto` | `nvenc` (h264_nvenc, on the GPU), `x264` (libx264 `-preset veryfast`), or `auto` (nvenc if a test encode works, else x264). |
| `bts.rec.crf` | `20` | Quality: x264 `-crf`, nvenc `-cq`. |
| `bts.rec.only` | all | Comma-separated scene-name prefixes; other scenes are skipped completely, setup always runs. For partial re-takes. |
| `bts.rec.distance` | `16` | Render distance in chunks. |
| `bts.rec.fov` | `70` | Field of view. |
| `bts.rec.exit` | `true` | Close the client when done. |

## Script format

One command per line. `#` starts a comment. Commands before the first `scene` are the **setup**, which is never
recorded. Each `scene <name>` starts a scene, written to its own segment. Inside a scene the commands run in order.

### Places

A place is an anchor with an optional offset, or absolute coordinates:

- `eye`: the Eye T1 rig's centre (where the star sits). `eyectrl`: the Eye's controller block.
- `kerr`: the Kerr T1 rig's centre (the hole). `keectrl`: the KEE controller block.
- `placed`: the controller the last `place` put down. `here`: the camera now.
- `eye+0,30,-80` is the anchor plus (dx, dy, dz). `1000.5,40,3990` is absolute.

The rigs face north (-z), so their fronts are on the -z side. Anchors exist once their `build` has run.

### Commands

| Command | Recorded | What it does |
|---|---|---|
| `scene <name>` | | Starts a scene (segment `NN_<name>.mp4`). |
| `caption <text>` | | The scene's caption: a semi-transparent bar at the bottom with white text, shown for the whole scene. Write it for someone who has never seen the mod: what they are looking at, in plain words. |
| `card <seconds> <title> \| <line> \| ...` | yes | Title or end card: dark background, centred text. The first line is large. The text fades in and out. |
| `hold <seconds>` | yes | Records the camera where it is. |
| `orbit <centre> radius <r> [height <h>] [from <deg>] [to <deg>] seconds <s> [look <place>] [ease inout\|linear\|in\|out]` | yes | Flies round the centre, looking at it (or at `look`). Angle 0 is north of the centre (-z) and 90 is east (+x). The camera is `h` above the centre. If the camera is not already at the start, a `goto` there runs first, unrecorded. |
| `dolly <from> <to> seconds <s> [look <place>] [ease ...]` | yes | Moves in a straight line, looking at `look` the whole way, or keeping the current direction. A `goto` to `<from>` runs first if needed. |
| `pan <place> seconds <s>` | yes | Turns the camera smoothly to look at the place. |
| `timelapse <video-seconds> [speed <x>] [sky]` | yes | Time-lapse: `video-seconds x speed` of real time recorded into `video-seconds` of video (default speed 20). Everything runs at its real rate (machines, redstone) and the video is sped up. `sky` turns the daylight cycle on for the time-lapse. A "TIME-LAPSE 20x" tag is drawn in the top-right corner. |
| `goto <place> [look <place>]` | | Teleports the camera (the server loads the chunks), then waits until the chunks have arrived and rendered. |
| `look <place>` | | Turns the camera to the place at once. |
| `gui <place> [scale <n>]` | | The server opens the GT machine GUI at that block for the player (the camera must be within about 8 blocks, so `goto` first). Waits until the screen is up. NEI's panels are hidden. GUI scale defaults to the video height / 360 (3 at 1080p, 2 at 720p). Follow it with `hold`, then `close`. |
| `questbook [<id>] [scale <n>]` | | Opens BetterQuesting's book: the chapter list, or one quest by its id (`48000` = high 190 low 48000, or `high:low`). Follow with `hold` and `close`. |
| `close` | | Closes the open screen. |
| `build eye\|kerr\|both` | | Builds the autoshots photo rigs (`AutoRig`): Rig A, the Eye T1 on redstone and GT covers, which lights itself about 200 s after the build; Rig B, the Kerr T1 on AE2. |
| `cell` | | Puts the 64k cell into Rig B's ME drive, which starts the Kerr. |
| `place <machine> <place> [facing <dir>]` | | Places a GT machine's controller. `<machine>` is `ItemList.<name>` (GT, e.g. `ItemList.Machine_Multi_BlastFurnace`) or `bts.<field>` (this addon's `BTSMachines`, e.g. `bts.eyeOfHarmony`). Sets the `placed` anchor. |
| `hologram <place> hints [tier <n>]` | | Uses a Hologram Projector on the controller as the player: the structure's hint blocks appear. |
| `hologram <place> build [tier <n>]` | | Builds the multiblock the way a creative player's sneak-use of the projector does. |
| `time day\|noon\|sunset\|night\|sunrise\|<ticks>` | | Sets the time of day. The daylight cycle is off, except during `timelapse ... sky`. |
| `wait eyelit\|holeopen [timeout <s>]` | | Waits until Rig A's star burns or Rig B's hole is open (default timeout 600 s). |
| `wait seconds <n>` | | Waits n real seconds. |
| `command <server command>` | | Runs a server command as the player (op), e.g. `command /btseye ...`. |

### Example

```
# setup
build both
time day

scene title
  card 3 Beyond the Stars | The Eye of Harmony and the Kerr engine

scene kerr-start
  caption The Kerr engine, tier 1, fed by an AE2 network: the cell goes in and the network sends the seed
  goto kerr+36,16,-58 look kerr+0,-6,-4
  hold 2
  cell
  timelapse 6 speed 10

scene eye-orbit
  caption The Eye of Harmony, tier 1: a star held in a magnetic field, kept stable by plain redstone
  wait eyelit timeout 900
  orbit eye+0,-6,-10 radius 75 height 28 from -40 to 40 seconds 14

scene eye-gui
  caption The Eye's controller screen: the star's state and the field gauges
  goto eyectrl+0,0.2,-3.5 look eyectrl
  gui eyectrl
  hold 4
  close

scene end
  card 3 Thanks for watching
```

## Limits

- Dev client only, in an integrated world it creates. It does not attach to an existing save or a dedicated server.
- `gui` opens GT machine GUIs only (anything whose tile is a GT `CommonMetaTileEntity`); the Eye's ModularUI screen
  was checked on video.
- `questbook` is not ready for a showcase. It opens BetterQuesting's chapter list, but in the feature test the book
  (a) was empty, because the dev run has no `config/betterquesting/DefaultQuests` and `/btsquests install` fails
  without it, and (b) was drawn in the top-left third of the frame (BetterQuesting's canvas scaling under the
  recorder; not investigated). `questbook <id>` for a quest that does not exist opens nothing (10 s timeout, logged).
  To film the book, merge `questbook/` into `run/client/config/betterquesting/` first and check the scaling.
- `hologram ... hints` shows StructureLib's hint blocks; they fade after StructureLib's hint lifetime, so
  record them soon after the command.
- A `timelapse` speeds up the video, not the game. A process that takes 15 minutes in game takes 15 minutes to record.
- The window must stay unminimised while recording.
