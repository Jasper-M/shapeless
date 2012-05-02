/*
 * Copyright (c) 2011 Miles Sabin 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.{ EclipseKeys, EclipseCreateSrc }

object ShapelessBuild extends Build {
  
  override lazy val settings = super.settings :+ (
    EclipseKeys.skipParents := false
  )

  lazy val shapeless = Project(
    id = "shapeless", 
    base = file("."),
    aggregate = Seq(shapelessCore, shapelessExamples),
    settings = commonSettings ++ Seq(
      moduleName := "shapeless-root",
        
      (unmanagedSourceDirectories in Compile) := Nil,
      (unmanagedSourceDirectories in Test) := Nil,
      
      publish := (),
      publishLocal := ()
    )
  )
  
  lazy val shapelessCore = {
    
    Project(
      id = "shapeless-core", 
      base = file("core"),
      settings = commonSettings ++ Publishing.settings ++ Seq(
        moduleName := "shapeless",
        
        (unmanagedSourceDirectories in Compile) <<= (scalaSource in Compile)(Seq(_)),
        (unmanagedSourceDirectories in Test) <<= (scalaSource in Test)(Seq(_)),
        
        (unmanagedSourceDirectories in Compile) <++= (scalaVersion, baseDirectory) { (sv, bd) =>
          if(pre210(sv)) Seq(bd / "src-2.9/main/scala") else Nil
        },
  
        managedSourceDirectories in Test := Nil,
        
        EclipseKeys.createSrc := EclipseCreateSrc.Default+EclipseCreateSrc.Managed,
        
        libraryDependencies ++= Seq(
          "com.novocode" % "junit-interface" % "0.7" % "test"
        ),
        
        (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen,

        mappings in (Compile, packageSrc) <++=
          (sourceManaged in Compile, managedSources in Compile) map { (base, srcs) =>
            (srcs x (Path.relativeTo(base) | Path.flat))
          },
          
        mappings in (Compile, packageSrc) <++=
          (mappings in (Compile, packageSrc) in LocalProject("shapeless-examples"))
      )
    )
  }
  
  lazy val shapelessExamples = Project(
    id = "shapeless-examples",
    base = file("examples"),
    dependencies = Seq(shapelessCore),
 
    
    settings = commonSettings ++ Seq(
      (unmanagedSourceDirectories in Compile) <<= (scalaSource in Compile)(Seq(_)),
      (unmanagedSourceDirectories in Test) <<= (scalaSource in Test)(Seq(_)),
      
      libraryDependencies ++= Seq(
        "com.novocode" % "junit-interface" % "0.7" % "test"
      ),

      publish := (),
      publishLocal := ()
    )
  )
  
  def pre210(sv : String) : Boolean = {
    val Some((major, minor)) = CrossVersion.partialVersion(sv)
    major < 2 || (major == 2 && minor < 10)
  }
  
  def commonSettings = Defaults.defaultSettings ++
    Seq(
      organization        := "com.chuusai",
      version             := "1.2.1-SNAPSHOT",
      scalaVersion        := "2.10.0-SNAPSHOT",
      crossScalaVersions  <<= version {
        v =>
          Seq("2.9.1", "2.9.1-1", "2.9.2", "2.10.0-M3") ++
            (if (v.endsWith("-SNAPSHOT")) Seq("2.10.0-SNAPSHOT") else Seq())
      },
      scalacOptions       <<= scalaVersion map { sv =>
        // -unchecked is too noisy with virtpatmat currently: see, 
        //  https://groups.google.com/d/msg/scala-internals/OQyffmwMJsg/DC3YZdN_QJIJ
        Seq("-deprecation") ++ (
          if (pre210(sv))
            Seq("-Ydependent-method-types", "-unchecked", "-deprecation")
          else
            Seq("-feature", "-language:higherKinds", "-language:implicitConversions")
        )
      },
      resolvers           ++= Seq(
        Classpaths.typesafeSnapshots,
        "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
      )
    )
}

