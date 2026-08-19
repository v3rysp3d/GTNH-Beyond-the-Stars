# Unit One — canonical plant geometry (as of 0.16.0 + TC ruling)

Frame: the main station controller sits at platform (a=25, c=17), pad level y=0. `a` is
the horizontal axis (0..49), `c` the depth axis (0=front, 49=back), everything rotates
with the controller's facing. `edge` = silo diameter = 7 + 2*tier (tiers 1..5: 9-17).

## Platform (mandatory)
- 50x50, two layers: floor layer at y=-1 (2243 Light Concrete 'O' + 249 Tungstensteel
  Reinforced 'B' shield-pad octagon, max-tier footprint a,c 17..33 + 8 flush Solid
  Steel markers 'M' under dock positions), walking level y=0 open.
- Controller at (25,17) on the pad's front line. Dock positions (clear until a module
  controller stands there, FACING AWAY from its building):
  {38,30} Turbine Hall (retired), {8,1} NW tower, {8,48} SW tower, spares {40,8},
  {40,42}, {25,43}, {10,25}, {25,7}.

## Steam Generator silo (optional piece, tier 1-5)
- Octagon edge n = 7+2t, corner cut CUT={-,2,2,3,3,4}, FRONT-ANCHORED: front row c=17,
  centered a=25 (spans a 25-n/2 .. 25+n/2, c 17 .. 16+n).
- Walls 2n tall from pad level, hemispherical dome n/2 on top. Wall chars by tier:
  Radiation-Proof (t1-2) / Europium-Reinforced (t3-4) / Radiant Naquadah Alloy (t5);
  radiation-proof band rings at bottom-up layers {2, (9+2n-1)/2, 2n-2}; 7 buttress ribs
  (front skipped). Dome shield: TS-Reinforced (t1-2) / Iridium TS (t3+).
- Interior: fuel pillar frames 8 tall (bottom-up layers 3..10), Rod Loading Ports layer
  11. Pillar caps per tier: 45/77/109/157/201.
- TC-1 SOCKETS: flat front face cells a 23..27, y 0..2 (all tiers):
  y0: 23=Input Hatch, 24=Maintenance, 25=MAIN CONTROLLER, 26=Output Hatch, 27=Input Hatch
  y1: 23=Energy, 24=Energy, 25=SILO CONTROL BLOCK, 26=Energy, 27=Energy
  y2: 23=Input Bus, 24=wall, 25=wall, 26=wall, 27=Output Bus
  All other wall cells are PLAIN casings (no hatches, no hints).

## The Spine duct (optional)
- 3x3 cross-section, y 0..2, rows centered on the silo mid-row c = 17+n/2 (rows
  c 16+n/2 .. 18+n/2), running a = 25+n/2+2 (flush past the east rib) to a=42
  (length 16-n/2). Tungstensteel Turbine Casing shell, hollow bore.
- 4 Turbine Housing sockets in the bore at a-offsets 1,3,5,7 from the silo end;
  right-click sets CW/ACW; swirl S accumulates: riding x(1+0.08|S|) speed,
  opposing x(1+0.10|S|) efficiency; residual |S|^2 x5 heat/s.

## The Spiral chamber (optional)
- 7x7x12 box, a 43..49, rows tier-mid +/-3 (c 14+n/2 .. 20+n/2), y 0..11. TT Turbine
  Casing shell, interior: helix of 5-cell Light Concrete arcs stepping down the 5x5
  interior ring (3 cells advance per level) around a 3x3 open downcomer.
- TC-1 SOCKETS: 8 Precision Injectors on the walls, one per interior level 1..8,
  rotating faces BACK/WEST/FRONT/EAST from level 1 (mid-cell). The rotation starts on
  the back face deliberately: back-face sockets then land at levels 1 and 5 (world y 10
  and 6), clear above the gallery's 5-tall front wall - a front-first rotation entombed
  the level-7 socket. Spine Sensor in the roof's front-center cell; 1 Input Hatch +
  1 Maintenance in the bottom wall's front corners. All other wall cells plain casings.

## The Turbine Generator (optional; TC redesign of the Rotor Assembly)
- Rotor gallery 7 wide x 5 tall x 15 long, a 43..49, c (21+n/2)..(35+n/2), hollow bore,
  8 rotor STAGE housings on the centerline at bore height y2: gallery rows 1,3,5,7,9,
  11,13 plus one hanging in the manifold mouth (row 15). Each housing (a tile entity)
  seats a placeable TURBINE ROTOR item (10 durability; -5% stage factor per damage
  point; shatters at 10; sneak-click empty-handed extracts, wear preserved).
- OUTPUT MANIFOLD at the south end: 7x7x3 (y 0..6), c (36+n/2)..(38+n/2), hollow; its
  front plane is walled except the mouth matching the gallery bore (a 1..5, y 1..3);
  4 Dynamo sockets mid-height on the back face (y 3, a 1,2,4,5).
- TC-1/2 SOCKETS: TURBINE CONTROL BLOCK on the MANIFOLD BACK FACE (last row, a 3,
  y 2) with 1 Maintenance directly below it (y 1) - the gallery's front wall is
  entombed against the Spiral's back wall, so a redstone line could never reach a
  control block there. All other cells plain TT casings.
- RPM model: redstone rising edge on the Turbine Control Block starts a cycle
  (consumes a 20xL/t steam slug + opens the +/-2t injector window); RPM gain =
  176 x P x timed/8, decay 0.5%/t, redline 12,000; EU/t = L/t x speed x eff x
  (RPM/1000) x avgRotorFactor (avg over all 8 sockets, so it carries stages/8),
  clamped to dynamo capacity; coolant ceil(200x(RPM/1000)^2/Q).

## Module buildings (own controllers on dock markers)
- Cooling Towers: 17x17x20 hyperboloid (base r7.5, waist r4.6 at 62%, top r5.6),
  Light Concrete shell, Solid Steel base ring, controller front of ring. NW body
  a 0..16 c 0..16; SW body a 0..16 c 33..49.
- Turbine Hall (RETIRED, museum): 5x4x9 at a 36..40, c 30..38.

## Color legend for mockups
concrete #9aa0a2, shield TS #9d8f6f, rad-proof #8a8f5a, europium #7a9b6a, radiant naq
#6a5a9b, solid steel #5d6b78, TT turbine casing #4a6d99, stainless #b8bfc4, fuel rod
#76c44e, housing CW #4a86c8 / ACW #8a5ac8, sockets: energy #d9a521, dynamo #d94a21,
injector #21b8d9, sensor #d921b8, control blocks #e8e8e8, maintenance #c87a4a.
