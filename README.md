<div align="center">
<img src="https://i.imgur.com/yVFgPpr.png" alt="Logo" width="250">
<h1>Flywheel</h1>
<h6>A modern engine for modded Minecraft.</h6>
<a href="https://discord.gg/xjD59ThnXy"><img src="https://img.shields.io/discord/841464837406195712?color=844685&label=Discord&style=flat" alt="Discord"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/flywheel"><img src="http://cf.way2muchnoise.eu/flywheel.svg" alt="Curseforge Downloads"></a>
<br>
</div>

### About
The goal of this project is to provide tools for mod developers so they no longer have to worry about performance, or limitations of Minecraft's archaic rendering engine.
That said, this is primarily an outlet for me to have fun with graphics programming.


### Instancing

So far, Flywheel provides an alternate, unified path for entity and tile entity rendering that takes advantage of GPU instancing. In doing so, Flywheel gives the developer the flexibility to define their own vertex and instance formats, and write custom shaders to ingest that data.

### Shaders
To accomodate the developer and leave more in the hands of the engine, Flywheel provides a custom shader loading and templating system to hide the details of the CPU/GPU interface. This system is a work in progress. There will be breaking changes, and I make no guarantees of backwards compatibility.