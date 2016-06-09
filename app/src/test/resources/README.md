# Multiple Repos Test

This covers the three indexes:
 * multiRepo.normal.jar
 * multiRepo.archive.jar
 * multiRepo.conflicting.jar

The goal is that F-Droid client should be able to:

 * Update all three repos successfully
 * Show all included versions for download in the UI
 * Somehow deal nicely with the fact that two repos provide versions 50-53 of AdAway

## multiRepo.normal.jar

 * 2048 (com.uberspot.a2048)
   - Version 1.96 (19)
   - Version 1.95 (18)
 * AdAway (org.adaway)
   - Version 3.0.2 (54)
   - Version 3.0.1 (53)
   - Version 3.0 (52)
 * adbWireless (siir.es.adbWireless)
   - Version 1.5.4 (12)

## multiRepo.archive.jar

 * AdAway (org.adaway)
   - Version 2.9.2 (51)
   - Version 2.9.1 (50)
   - Version 2.9 (49)
   - Version 2.8.1 (48)
   - Version 2.7 (46)
   - Version 2.6 (45)
   - Version 2.3 (42)
   - Version 2.1 (40)
   - Version 1.37 (37)
   - Version 1.35 (36)
   - Version 1.34 (35)

## multiRepo.conflicting.jar

 * AdAway (org.adaway)
   - Version 3.0.1 (53)
   - Version 3.0 (52)
   - Version 2.9.2 (51)
   - Version 2.2.1 (50)
 * Add to calendar (org.dgtale.icsimport)
   - Version 1.2 (3)
   - Version 1.1 (2)