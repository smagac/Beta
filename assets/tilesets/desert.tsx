<?xml version="1.0" encoding="UTF-8"?>
<tileset name="desert" tilewidth="32" tileheight="32">
 <image source="tilesets/desert.png" width="96" height="96"/>
 <tile id="0">
  <properties>
   <property name="block" value="true"/>
   <property name="name" value="wall"/>
   <property name="density" value=".3"/>
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
   <property name="density" value=".2"/>
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
  <property name="weather" value=".35 sandstorm.png 2.7 .02"/>
 </properties>
</tileset>
