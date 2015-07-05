<?xml version="1.0" encoding="UTF-8"?>
<tileset name="snow" tilewidth="32" tileheight="32">
 <image source="data/tilesets/snow.png" width="96" height="96"/>
 <tile id="0">
  <properties>
   <property name="block" value="true"/>
   <property name="name" value="wall"/>
   <property name="density" value=".4"/>
  </properties>
 </tile>
 <tile id="1">
  <properties>
   <property name="block" value="false"/>
   <property name="name" value="floor"/>
  </properties>
 </tile>
 <tile id="2">
  <properties>
   <property name="block" value="true"/>
   <property name="name" value="null"/>
   <property name="density" value=".3"/>
  </properties>
 </tile>
 <tile id="3">
  <properties>
   <property name="block" value="false"/>
   <property name="name" value="up"/>
  </properties>
 </tile>
 <tile id="5">
  <properties>
   <property name="block" value="false"/>
   <property name="name" value="down"/>
  </properties>
 </tile>
 <properties>
  <property name="weather_strong" value=".2 blizzard.png .6 -.9"/>
  <property name="weather_weak" value=".4 snow.particle 0 1 .25 1 .5 1 .75 1 1 1" />
 </properties>
</tileset>
