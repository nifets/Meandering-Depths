# Meandering-Depths

A small project of mine, using Scala with LWJGL to create a 3D game of some kind. The main idea is having a character that explores an extensive cave system that expands "infinitely" in all directions. Not sure how the gameplay will work, some ideas would be trying to be as stealthy as possible, so as to not get noticed by the hostile creatures, and perhaps one goal of the game would be to escape the caves.

Click to see video:
[![Screenshot3](/src/main/resources/images/md3.png "Screenshot3")](https://www.youtube.com/watch?v=mQZDJdRH0ZQ)

Features:
-
-Unbounded procedural terrain generation using the Marching Cubes algorithm on the GPU (extracting a polygonal mesh of an isosurface from a 3D noise function)

-Terrain loading done in parallel with rendering, using a chunk loading system

-Simple terrain collision detection optimized to make use of its regular grid structure 

-GPU-accelerated skeletal animation

-3rd person camera using quaternions for orientation

-Simple lighting and material system; fog

-Input system that abstracts away the raw input from game actions


To do:
-
-Inverse kinematics using Jacobians or FABRIK

-State machine to represent current player state and animation (idle, walking, jumping, falling, etc.)

-Encapsulate rendering

-Improve terrain gen --  add biomes, stalactites/stalagmites, flora (maybe using L-systems), etc.

-Try out smooth shading and see if it looks better than flat shading 

-Entities 

-Hive mind creature that uses a neural network to learn to hide when the player is nearby (trained using screenshots of the game from the creature's perspectives)

![Screenshot](/src/main/resources/images/md1.png "Screenshot")

![Screenshot2](/src/main/resources/images/md2.png "Screenshot2")
