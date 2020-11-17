# CHANGELOG

## 4.0

 - TODO: Prevent teleporting into an arena

 - Updated to Spigot API 1.14, will no longer work on previous versions. May also require Magic 8.1 or higher.
 - Add item_wear arena property, to turn off items and armor getting worn in a battle
 - Add allow_consuming arena property
 - Fix arena config changes not saving when setting some properties
 - Add leaderboard_sign_type parameter to arenas

## 3.4

 - Fix inventory click errors in 1.14
 - OP wand check will now allow cost-free wands
 - Fix leaderboards in modern spigot versions

## 3.3

 - Depends on Magic 7.5.3 or higher
 - Maybe support for 1.13
 - Fix server lag when generating leaderboards or showing leaderboard GUI

## 3.2

 - Depends on Magic 6.9.13 or higher
 - Add Vault currency support for rewards

## 3.1

 - Depends on Magic 6.9.9 or higher
 - Added support for mob arenas:
   /arena configure MobArena add mob_spawns
   /arena configure MobArena add mob
 - Move leaderboard save data to a separate file

## 3.0
 - Depends on Magic 3.4
 - Build against Bukkit 1.8
 - Arenas can reward Magic SP on win/lose/draw

## 1.8

 - Depends on Magic 1.8
 - Added tracking of wins/losses
 - Added leaderboard tracking
 - Implement physical leaderboard
 - Implement chest inventory full leaderboard view
 - Add XP rewards for win/loss/draw
 - Implement asynchronous autosave

## 1.0

 - First Release, Magic-independent
 - Manages multiplayer dueling or spleef arenas
 - First "trap" - damaging portal blocks
