This is a fork of justinwon777's HumanCompanions mod, with a few AI fixes and new quirks. I'm only working on Forge 1.20.1 

An AI is involved in the work.

- fixed leveled-up companions losing some health upon cross-dimention teleport or restarting the game.
- tried to fix companions stuck attacking a mob it can't reach.
- companions teleports to owner if they're over 24 blocks away regardless if they're still in a fight (so they'll be less likely to get lost if they did get stuck attacking anything.)
- companions get damage absorption in proportion to food saturation. Before, they only recover health according to food hunger value, making rotten flesh the best food for them, which doesn't sit right with me so I buffed the better foods.
- companions no longer eat uncooked meat or food that may provide harmful effects (except rotten flesh, that's a special case cuz sometimes we all have to rely on those.)
- tamed companions can pick up food, ore, or anything already has a copy in their backpack (gears they're equipping aren't counted), practically help collecting loot. 
- tamed companions try to cook food on campfire when player sleeps to skip the night, if they have a campfire in their inventory, or if there is a lit campfire block nearby. 
- <s>tried to add epicfight compatibility</s> this might not actually work. I can't figure where epicfight datapatch goes it seems to change with each update. I tried okay x_x


Below are the original links and discriptions.

------------

# About

This mod adds new human companions that can help you on your journey. Currently available for Forge 1.16.5, 1.18.2, 1.19.2, and 1.20.1.

## Features

- Knight, archer, axeguard, and arbalist (crossbow) companions
- Find them in the wild by their house
- Give them food to have them join you
- Will fight for you and can carry items (don't recommend storing valuables in them as they die easily)
- Make them follow you or patrol an area

## Quick Guide

Companions can have different looks. Some are shown here:

<img src="https://github.com/justinwon777/HumanCompanions/blob/main/models.png" alt="models" width="600">

They live in a wide variety of biomes in houses like these (plus more!):

<img src="https://github.com/justinwon777/HumanCompanions/blob/main/houses.png" alt="house" width="800">

- Right click on them to see what food they want.
- Give them food by right clicking them with the food in hand and once their request is fulfilled, they will become your companion.
- Once they join you, you can access their inventory with right click.
- They have names, but you can rename them with tags.
- Shift+right click to toggle "sit", making them stay in place and not do anything.
- Give them armor and weapons and they will automatically equip the best one in their inventory. They can only use the weapon they spawn with. Archers and arbalists do not need arrows.
- They will follow you around the same way a wolf does. They will attack mobs that hurt you or you hurt, except creepers. They will give a warning if a creeper is nearby and run away.
- Put food in their inventory and they will eat to heal themselves.
- Toggle "Hunt" and "Alert" modes in their inventory to control their behavior.
- Toggle between follow, patrol, and guard states
- Toggle stationery to keep archers and arbalists standing while shooting
- You could do "/locate humancompanions:companion_house" to find a house quickly.
- Track your companion locations with the [Pet Tracker](https://www.curseforge.com/minecraft/mc-mods/pet-tracker "pet tracker") mod.
- Companions level up by killing mobs (friendly or hostile). Every 3 levels, their health will increases by 1.

## Known Issues

- When a companion dies, sometimes it drops a duplicate of an item or doesn't drop an item in its inventory at all.
- Archers sometimes shoot at nothing after killing a mob. Use clear button to clear target.
- Spawn eggs might spawn a baby. They can't do anything and will despawn when the game reloads.
- Knights sometimes hesitate while attacking.

## Links

Join the discord to ask questions or help contribute https://discord.gg/RmTYpsAhCV

Support me on [Patreon.](https://www.patreon.com/jus10wontons "patreon")