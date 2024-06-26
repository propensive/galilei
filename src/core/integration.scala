/*
    Galilei, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package anticipation

import serpentine.*
import spectacular.*
import galilei.*

import language.experimental.pureFunctions

package filesystemApi:
  given [PathType <: Path](using hierarchy: Hierarchy[PathType, ?])(using Decoder[PathType], PathResolver[File, PathType]) => File is SpecificFile & GenericFile as galileiFile =
    new SpecificFile with GenericFile:
      type Self = File
      def file(name: Text): File = name.decodeAs[PathType].as[File]
      def fileText(file: File): Text = file.path.fullname

  given [PathType <: Path](using hierarchy: Hierarchy[PathType, ?], decoder: Decoder[PathType]) => PathType is SpecificPath & GenericPath as galileiPath =
    new SpecificPath with GenericPath:
      type Self = PathType
      def path(name: Text): PathType = name.decodeAs[PathType]
      def pathText(path: PathType): Text = path.fullname

  given [PathType <: Path](using hierarchy: Hierarchy[PathType, ?])(using Decoder[PathType], PathResolver[Directory, PathType]) => Directory is SpecificDirectory & GenericDirectory as galileiDirectory =
    new SpecificDirectory with GenericDirectory:
      type Self = Directory
      def directory(name: Text): Directory = name.decodeAs[PathType].as[Directory]
      def directoryText(directory: Directory): Text = directory.path.fullname
