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

package galilei

import anticipation.*
import contextual.*
import contingency.*
import fulminate.*
import galilei.*
import gossamer.*
import guillotine.*
import kaleidoscope.*
import rudiments.*
import serpentine.*
import spectacular.*
import symbolism.*
import turbulence.*
import vacuous.*

import scala.compiletime.*
import scala.jdk.StreamConverters.*

import java.io as ji
import java.nio as jn
import java.nio.file as jnf
import java.nio.file.attribute as jnfa
import java.nio.channels as jnc

import language.experimental.pureFunctions

type GeneralForbidden = Windows.Forbidden | Unix.Forbidden

object Path:
  given Path is GenericPath = _.fullname

  inline given add(using path: Tactic[PathError], followable: Link is Followable[GeneralForbidden, ?, ?])
          : Addable with
    type Self = Path
    type Operand = Link

    type Result = Path
    inline def add(left: Path, right: Link): Path = left.append(right)

  inline given add2(using path: Tactic[PathError], followable: SafeLink is Followable[GeneralForbidden, ?, ?])
          : Addable with
    type Self = Path
    type Operand = SafeLink

    type Result = Path
    inline def add(left: Path, right: SafeLink): Path = left.append(right)

  given Insertion[Sh.Parameters, Path] = path => Sh.Parameters(path.fullname)

  given (using io: Tactic[IoError], streamCut: Tactic[StreamError]) => Path is Writable by Bytes as writableBytes =
    Writable.outputStreamBytes.contramap: path =>
      if !path.stdlib.toFile.nn.canWrite then abort(IoError(path))
      ji.BufferedOutputStream(ji.FileOutputStream(path.stdlib.toFile, false))

  given Path is Navigable[GeneralForbidden, Optional[Windows.Drive]] as navigable:
    def root(path: Path): Optional[Windows.Drive] = path match
      case path: Windows.SafePath => path.drive
      case path: Windows.Path     => path.drive
      case _                      => Unset

    def prefix(root: Optional[Windows.Drive]): Text =
      root.let(Windows.Path.navigable.prefix(_)).or(Unix.Path.navigable.prefix(Unset))

    def descent(path: Path): List[Name[GeneralForbidden]] =
      // FIXME: This is a bit of a hack
      import strategies.throwUnsafely
      path match
        case path: Unix.SafePath    => path.safeDescent
        case path: Windows.SafePath => path.safeDescent
        case path: Unix.Path        => path.descent.map(_.narrow[GeneralForbidden])
        case path: Windows.Path     => path.descent.map(_.narrow[GeneralForbidden])

    def separator(path: Path): Text = path match
      case path: Unix.SafePath    => t"/"
      case path: Unix.Path        => t"/"
      case path: Windows.SafePath => t"\\"
      case path: Windows.Path     => t"\\"

  given rootParser: RootParser[Path, Optional[Windows.Drive]] = text =>
    Windows.Path.rootParser.parse(text).or(Unix.Path.rootParser.parse(text))

  given PathCreator[Path, GeneralForbidden, Optional[Windows.Drive]] with
    def path(root: Optional[Windows.Drive], descent: List[Name[GeneralForbidden]]) = root match
      case drive@Windows.Drive(_) => Windows.SafePath(drive, descent)
      case _                      => Unix.SafePath(descent)

  given Path is Communicable = path => Message(path.render)

  inline given (using Tactic[PathError]) => Decoder[Path] as decoder:
    def decode(text: Text): Path = Navigable.decode(text)

  given Path is Showable as showable = _.render
  given encoder: Encoder[Path] = _.render
  given Path is Inspectable = _.render

  inline def apply[PathType: GenericPath](path: PathType): Path raises PathError =
    Navigable.decode(path.pathText)

sealed trait Path:
  this: Path =>

  def fullname: Text
  def name: Text
  def stdlib: jnf.Path = jnf.Path.of(fullname.s).nn
  def exists(): Boolean = jnf.Files.exists(stdlib)
  def touch()(using Tactic[IoError]): Unit = jnf.Files.write(stdlib, Array[Byte]())

  def wipe()(using deleteRecursively: DeleteRecursively)(using io: Tactic[IoError]): Path = this.also:
    deleteRecursively.conditionally(this)(jnf.Files.deleteIfExists(stdlib))

  def entryType()(using dereferenceSymlinks: DereferenceSymlinks)(using io: Tactic[IoError])
          : PathStatus =

    try (jnf.Files.getAttribute(stdlib, "unix:mode", dereferenceSymlinks.options()*): @unchecked) match
      case mode: Int => (mode & 61440) match
        case  4096 => PathStatus.Fifo
        case  8192 => PathStatus.CharDevice
        case 16384 => PathStatus.Directory
        case 24576 => PathStatus.BlockDevice
        case 32768 => PathStatus.File
        case 40960 => PathStatus.Symlink
        case 49152 => PathStatus.Socket
        case _     => throw Panic(m"an unexpected POSIX mode value was returned")

    catch
      case error: UnsupportedOperationException =>
        throw Panic(m"the file attribute unix:mode could not be accessed")

      case error: ji.FileNotFoundException =>
        raise(IoError(this), PathStatus.File)

      case error: ji.IOException =>
        raise(IoError(this), PathStatus.File)

  def as[EntryType <: Entry](using resolver: PathResolver[EntryType, this.type]): EntryType = resolver(this)

  inline def at[EntryType <: Entry](using PathResolver[EntryType, this.type], DereferenceSymlinks)
          : Optional[EntryType] raises IoError =
    if is[EntryType] then as[EntryType] else Unset

  inline def is[EntryType <: Entry](using DereferenceSymlinks, Tactic[IoError]): Boolean =
    inline erasedValue[EntryType] match
      case _: Directory   => entryType() == PathStatus.Directory
      case _: File        => entryType() == PathStatus.File
      case _: Symlink     => entryType() == PathStatus.Symlink
      case _: Socket      => entryType() == PathStatus.Socket
      case _: Fifo        => entryType() == PathStatus.Fifo
      case _: BlockDevice => entryType() == PathStatus.BlockDevice
      case _: CharDevice  => entryType() == PathStatus.CharDevice

  def make[EntryType <: Entry]()(using maker: EntryMaker[EntryType, this.type]): EntryType = maker(this)

object Link:
  given creator: PathCreator[Link, GeneralForbidden, Int] with
    def path(ascent: Int, descent: List[Name[GeneralForbidden]]): SafeLink = SafeLink(ascent, descent)

  inline given (using Tactic[PathError]) => Decoder[Link] as decoder:
    def decode(text: Text): Link =
      if text.contains(t"\\") then text.decodeAs[Windows.Link] else text.decodeAs[Unix.Link]

  given Link is Showable as showable =
    case link: Unix.Link    => link.render
    case link: Windows.Link => link.render
    case link: SafeLink     => link.render

  given encoder: Encoder[Link] = showable.text(_)
  given Link is Inspectable = showable.text(_)

sealed trait Link

object Windows:
  type Forbidden = ".*[\\cA-\\cZ].*" | "(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\.*)?" |
      "\\.\\." | "\\." | ".*[:<>/\\\\|?\"*].*"

  object Path:
    inline given (using Tactic[PathError]) => Decoder[Path] as decoder:
      def decode(text: Text): Path = Navigable.decode(text)

    given Path is Navigable[Forbidden, Drive] as navigable:
      def root(path: Path): Drive = path.drive
      def prefix(drive: Drive): Text = t"${drive.letter}:\\"
      def descent(path: Path): List[Name[Forbidden]] = path.descent
      def separator(path: Path): Text = t"\\"

    given creator: PathCreator[Path, Forbidden, Drive] = Path(_, _)

    given rootParser: RootParser[Path, Drive] = text => text.only:
      case r"$letter([a-zA-Z]):\\.*" => (Drive(unsafely(letter.at(0).vouch).toUpper), text.drop(3))

    given Path is Showable as showable = _.render
    given encoder: Encoder[Path] = _.render
    given Path is Inspectable = _.render

  case class Path(drive: Drive, descent: List[Name[Forbidden]]) extends galilei.Path:
    def root: Drive = drive
    def name: Text = if descent.isEmpty then drive.name else descent.head.show
    def fullname: Text = t"${Path.navigable.prefix(drive)}${descent.reverse.map(_.render).join(t"\\")}"

  class SafePath(drive0: Drive, val safeDescent: List[Name[GeneralForbidden]])
  extends Path(drive0, safeDescent.map(_.widen[Forbidden]))

  object Link:
    given creator: PathCreator[Link, Forbidden, Int] = Link(_, _)

    given (using ValueOf["."]) => Link is Followable[Forbidden, "..", "."] as followable:
      val separators: Set[Char] = Set('\\')
      def separator(path: Link): Text = t"\\"
      def ascent(path: Link): Int = path.ascent
      def descent(path: Link): List[Name[Forbidden]] = path.descent

    inline given decoder(using Tactic[PathError]): Decoder[Link] = Followable.decoder[Link]
    given Link is Showable as showable = _.render
    given encoder: Encoder[Link] = _.render
    given Link is Inspectable = _.render

  object Drive:
    given Drive is Inspectable = drive => t"drive:${drive.letter}"
    given Drive is Showable as showable = drive => t"${drive.letter}"
    given default: Default[Drive] = () => Drive('C')

  case class Drive(letter: Char):
    def name: Text = t"$letter:"

    @targetName("child")
    infix def / (name: Name[Forbidden]): Path = Path(this, List(name))

    @targetName("child2")
    inline infix def / (name: Text)(using Tactic[PathError]): Path = Path(this, List(Name(name)))

  case class Link(ascent: Int, descent: List[Name[Forbidden]]) extends galilei.Link

  sealed trait Entry extends galilei.Entry

object Unix:

  type Forbidden = ".*\\/.*" | ".*[\\cA-\\cZ].*" | "\\.\\." | "\\."

  @targetName("child")
  infix def / (name: Name[Forbidden]): Path = Path(List(name))

  @targetName("child2")
  inline infix def / (name: Text)(using Tactic[PathError]): Path = Path(List(Name(name)))

  object Path:
    given Path is Radical as radical = () => Path(Nil)

    inline given (using Tactic[PathError]) => Decoder[Path] as decoder:
      def decode(text: Text): Path = Navigable.decode(text)

    given rootParser: RootParser[Path, Unset.type] = text =>
      if text.starts(t"/") then (Unset, text.drop(1)) else Unset

    given creator: PathCreator[Path, Forbidden, Unset.type] = (root, descent) => Path(descent)

    given Path is Navigable[Forbidden, Unset.type] as navigable:
      def separator(path: Path): Text = t"/"
      def root(path: Path): Unset.type = Unset
      def prefix(root: Unset.type): Text = t"/"
      def descent(path: Path): List[Name[Forbidden]] = path.descent

    given Path is Showable as showable = _.render
    given encoder: Encoder[Path] = _.render
    given Path is Inspectable = _.render

  case class Path(descent: List[Name[Forbidden]]) extends galilei.Path:
    def root: Unset.type = Unset
    def name: Text = if descent.isEmpty then Path.navigable.prefix(Unset) else descent.head.show
    def fullname: Text = t"${Path.navigable.prefix(Unset)}${descent.reverse.map(_.render).join(t"/")}"

  class SafePath(val safeDescent: List[Name[GeneralForbidden]])
  extends Path(safeDescent.map(_.widen[Forbidden]))

  object Link:
    given creator: PathCreator[Link, Forbidden, Int] = Link(_, _)

    given (using ValueOf["."]) => Link is Followable[Forbidden, "..", "."] as followable:
      val separators: Set[Char] = Set('/')
      def separator(path: Link): Text = t"/"
      def ascent(path: Link): Int = path.ascent
      def descent(path: Link): List[Name[Forbidden]] = path.descent

    inline given (using Tactic[PathError]) => Decoder[Link] as decoder = Followable.decoder[Link]
    given Link is Showable as showable = _.render
    given encoder: Encoder[Link] = _.render
    given Link is Inspectable = _.render

  case class Link(ascent: Int, descent: List[Name[Forbidden]]) extends galilei.Link

  sealed trait Entry extends galilei.Entry

object Volume:
  given Volume is Inspectable = volume => t"volume[${volume.name}:${volume.volumeType}]"

case class Volume(name: Text, volumeType: Text)

sealed trait Entry:
  def path: Path
  def fullname: Text = path.fullname
  def stillExists(): Boolean = path.exists()

  def touch()(using Tactic[IoError]): Unit =
    try jnf.Files.setLastModifiedTime(path.stdlib, jnfa.FileTime.fromMillis(System.currentTimeMillis))
    catch case error: ji.IOException => raise(IoError(path))

  def hidden()(using Tactic[IoError]): Boolean =
    try jnf.Files.isHidden(path.stdlib) catch case error: ji.IOException => raise(IoError(path), false)

  object readable:
    def apply(): Boolean = jnf.Files.isReadable(path.stdlib)
    def update(status: Boolean): Unit = path.stdlib.toFile.nn.setReadable(status)

  object writable:
    def apply(): Boolean = jnf.Files.isWritable(path.stdlib)
    def update(status: Boolean): Unit = path.stdlib.toFile.nn.setWritable(status)

  object executable:
    def apply(): Boolean = jnf.Files.isExecutable(path.stdlib)
    def update(status: Boolean): Unit = path.stdlib.toFile.nn.setExecutable(status)

  def hardLinks()(using dereferenceSymlinks: DereferenceSymlinks, io: Tactic[IoError]): Int =
    try jnf.Files.getAttribute(path.stdlib, "unix:nlink", dereferenceSymlinks.options()*) match
      case count: Int => count
      case _          => raise(IoError(path), 1)
    catch case error: IllegalArgumentException => raise(IoError(path), 1)

  def volume: Volume =
    val fileStore = jnf.Files.getFileStore(path.stdlib).nn
    Volume(fileStore.name.nn.tt, fileStore.`type`.nn.tt)

  def delete()(using deleteRecursively: DeleteRecursively, io: Tactic[IoError]): Path =
    try deleteRecursively.conditionally(path)(jnf.Files.delete(path.stdlib)) catch
      case error: jnf.NoSuchFileException        => raise(IoError(path))
      case error: ji.FileNotFoundException       => raise(IoError(path))
      case error: ji.IOException                 => raise(IoError(path))
      case error: SecurityException              => raise(IoError(path))

    path

  def symlinkTo(destination: Path)
      (using overwritePreexisting: OverwritePreexisting, createNonexistentParents: CreateNonexistentParents)
      (using io: Tactic[IoError])
          : Path/*^{io, overwritePreexisting, createNonexistentParents}*/ =

    createNonexistentParents(destination):
      overwritePreexisting(destination):
        jnf.Files.createSymbolicLink(destination.stdlib, path.stdlib)

    destination

  def copyInto(destination: Directory)
      (using overwritePreexisting: OverwritePreexisting, dereferenceSymlinks: DereferenceSymlinks)
      (using io: Tactic[IoError])
          : Path/*^{io, overwritePreexisting, dereferenceSymlinks}*/ =

    given CreateNonexistentParents = filesystemOptions.createNonexistentParents
    copyTo(destination / path.descent.head)

  def copyTo(destination: Path)
      (using overwritePreexisting:     OverwritePreexisting,
             dereferenceSymlinks:      DereferenceSymlinks,
             createNonexistentParents: CreateNonexistentParents)
      (using io: Tactic[IoError])
          : Path/*^{io, overwritePreexisting, createNonexistentParents, dereferenceSymlinks}*/ =

    createNonexistentParents(destination):
      overwritePreexisting(destination):
        jnf.Files.copy(path.stdlib, destination.stdlib, dereferenceSymlinks.options()*)

    destination

  def moveInto
      (destination: Directory)
      (using overwritePreexisting: OverwritePreexisting,
             moveAtomically:       MoveAtomically,
             dereferenceSymlinks:  DereferenceSymlinks)
      (using io: Tactic[IoError])
          : Path/*^{io, overwritePreexisting, moveAtomically, dereferenceSymlinks}*/ =

    given CreateNonexistentParents = filesystemOptions.createNonexistentParents
    moveTo(destination / path.descent.head)

  def moveTo
      (destination: Path)
      (using overwritePreexisting:     OverwritePreexisting,
             moveAtomically:           MoveAtomically,
             dereferenceSymlinks:      DereferenceSymlinks,
             createNonexistentParents: CreateNonexistentParents)
      (using io: Tactic[IoError])
          : Path/*^{io, overwritePreexisting, createNonexistentParents, moveAtomically, dereferenceSymlinks}*/ =

    val options: Seq[jnf.CopyOption] = dereferenceSymlinks.options() ++ moveAtomically.options()

    createNonexistentParents(destination):
      overwritePreexisting(destination):
        jnf.Files.move(path.stdlib, destination.stdlib, options*)

    destination

  def lastModified[InstantType: SpecificInstant]: InstantType =
    SpecificInstant(jnf.Files.getLastModifiedTime(path.stdlib).nn.toInstant.nn.toEpochMilli)

object PathResolver:
  // given entry
  //     (using dereferenceSymlinks: DereferenceSymlinks, io: Tactic[IoError])
  //     : PathResolver[Entry, Path] =
  //   new PathResolver[Entry, Path]:
  //     def apply(path: Path): Entry =
  //       if path.exists() then path.entryType() match
  //         case PathStatus.Directory => Directory(path)
  //         case _                    => File(path)
  //       else raise(IoError(path))(Directory(path))

  given file
      (using createNonexistent:   CreateNonexistent,
             dereferenceSymlinks: DereferenceSymlinks,
             io:                  Tactic[IoError])
          : PathResolver[File, Path] = path =>

    if path.exists() && path.entryType() == PathStatus.File then File(path)
    else createNonexistent(path)(jnf.Files.createFile(path.stdlib))

    File(path)

  given directory
      (using createNonexistent: CreateNonexistent,
             dereferenceSymlinks: DereferenceSymlinks,
             io: Tactic[IoError])
          : PathResolver[Directory, Path] = path =>
    if path.exists() && path.entryType() == PathStatus.Directory then Directory(path)
    else createNonexistent(path):
      jnf.Files.createDirectory(path.stdlib)

    Directory(path)

@capability
trait PathResolver[+EntryType <: Entry, -PathType <: Path]:
  def apply(value: PathType): EntryType

object EntryMaker:
  given directory
      (using createNonexistentParents: CreateNonexistentParents, overwritePreexisting: OverwritePreexisting)
      (using io: Tactic[IoError])
          : EntryMaker[Directory, Path] = path =>
    createNonexistentParents(path):
      overwritePreexisting(path):
        jnf.Files.createDirectory(path.stdlib)

    Directory(path)

  given socket
      (using createNonexistentParents: CreateNonexistentParents, overwritePreexisting: OverwritePreexisting)
      (using io: Tactic[IoError])
          : EntryMaker[Socket, Unix.Path] = path =>
    createNonexistentParents(path):
      overwritePreexisting(path):
        val address = java.net.UnixDomainSocketAddress.of(path.stdlib).nn
        val channel = jnc.ServerSocketChannel.open(java.net.StandardProtocolFamily.UNIX).nn
        channel.bind(address)
        Socket(path, channel)

  given file
      (using createNonexistentParents: CreateNonexistentParents, overwritePreexisting: OverwritePreexisting)
          : EntryMaker[File, Path] =
    path => createNonexistentParents(path):
      overwritePreexisting(path):
        jnf.Files.createFile(path.stdlib)

    File(path)

  given fifo
      (using createNonexistentParents: CreateNonexistentParents,
             overwritePreexisting:     OverwritePreexisting,
             working:                  WorkingDirectory,
             io:                       Tactic[IoError],
             exec:                     Tactic[ExecError])
          : (EntryMaker[Fifo, Unix.Path] logs IoEvent) =

    path => createNonexistentParents(path):
      overwritePreexisting(path):
        sh"mkfifo $path"() match
          case ExitStatus.Ok => ()
          case _             => raise(IoError(path))

    Fifo(path)

@capability
trait EntryMaker[+EntryType <: Entry, -PathType <: Path]:
  def apply(value: PathType): EntryType

object Directory:
  given Directory is Inspectable = directory => t"directory:${directory.path.render}"

  given GenericWatchService:
    def apply(): java.nio.file.WatchService = jnf.Path.of("/").nn.getFileSystem.nn.newWatchService().nn

  given Directory is GenericDirectory = _.path.fullname

case class Directory(path: Path) extends Unix.Entry, Windows.Entry:
  def children: LazyList[Path] = jnf.Files.list(path.stdlib).nn.toScala(LazyList).map: child =>
    path / Name.unsafe(child.getFileName.nn.toString.nn.tt)

  def descendants(using DereferenceSymlinks, Tactic[IoError], PathResolver[Directory, Path]): LazyList[Path] =
    children #::: children.filter(_.is[Directory]).map(_.as[Directory]).flatMap(_.descendants)

  def size()(using PathResolver[Directory, Path], PathResolver[File, Path]): ByteSize raises IoError =
    import filesystemOptions.doNotDereferenceSymlinks
    descendants.map(_.at[File].let(_.size()).or(0.b)).foldLeft(0.b)(_ + _)

  @targetName("child")
  infix def / (name: Name[GeneralForbidden]): Path = path / name

  @targetName("child2")
  inline infix def / (name: Text)(using Tactic[PathError]): Path = path / Name(name)

object File:
  given File is Inspectable as inspectable = file => t"file:${file.path.render}"

  given [FileType <: File](using Tactic[StreamError], Tactic[IoError])
      => FileType is Readable by Bytes as readableBytes =
    Readable.inputStream.contramap: file =>
      try ji.BufferedInputStream(jnf.Files.newInputStream(file.path.stdlib))
      catch case _: jnf.NoSuchFileException => abort(IoError(file.path))

  given (using io: Tactic[IoError], streamCut: Tactic[StreamError])
      => File is Writable by Bytes as writableBytes =

    Writable.outputStreamBytes.contramap: file =>
      if !file.writable() then abort(IoError(file.path))
      ji.BufferedOutputStream(ji.FileOutputStream(file.path.stdlib.toFile, false))

  given (using io: Tactic[IoError], streamCut: Tactic[StreamError])
      => File is Appendable by Bytes as appendableBytes =
    Appendable.outputStreamBytes.contramap: file =>
      if !file.writable() then abort(IoError(file.path))
      ji.BufferedOutputStream(ji.FileOutputStream(file.path.stdlib.toFile, true))

  given File is GenericFile = _.path.fullname

case class File(path: Path) extends Unix.Entry, Windows.Entry:
  def size(): ByteSize = jnf.Files.size(path.stdlib).b

  def hardLinkTo(destination: Path)
      (using overwritePreexisting: OverwritePreexisting, createNonexistentParents: CreateNonexistentParents)
      (using io: Tactic[IoError])
          : Path/*^{io, overwritePreexisting, createNonexistentParents}*/ =

    createNonexistentParents(destination):
      overwritePreexisting(destination):
        jnf.Files.createLink(destination.stdlib, path.stdlib)

    destination

object Socket:
  given Socket is Inspectable = socket => t"socket:${socket.path.render}"

case class Socket(path: Unix.Path, channel: jnc.ServerSocketChannel) extends Unix.Entry

object Fifo:
  given Fifo is Inspectable = fifo => t"fifo:${fifo.path.render}"

case class Fifo(path: Unix.Path) extends Unix.Entry

object Symlink:
  given Symlink is Inspectable = symlink => t"symlink:${symlink.path.render}"

case class Symlink(path: Unix.Path) extends Unix.Entry

object BlockDevice:
  given BlockDevice is Inspectable = device => t"block-device:${device.path.render}"

case class BlockDevice(path: Unix.Path) extends Unix.Entry

object CharDevice:
  given CharDevice is Inspectable = device => t"char-device:${device.path.render}"

case class CharDevice(path: Unix.Path) extends Unix.Entry

enum PathStatus:
  case Fifo, CharDevice, Directory, BlockDevice, File, Symlink, Socket

object DereferenceSymlinks:
  given default(using Quickstart): DereferenceSymlinks = filesystemOptions.dereferenceSymlinks

@capability
trait DereferenceSymlinks:
  def options(): List[jnf.LinkOption]

object MoveAtomically:
  given default(using Quickstart): MoveAtomically = filesystemOptions.doNotMoveAtomically

@capability
trait MoveAtomically:
  def options(): List[jnf.CopyOption]

object CopyAttributes:
  given default(using Quickstart): CopyAttributes = filesystemOptions.doNotCopyAttributes

@capability
trait CopyAttributes:
  def options(): List[jnf.CopyOption]

object DeleteRecursively:
  given default(using Quickstart, Tactic[UnemptyDirectoryError]): DeleteRecursively =
    filesystemOptions.doNotDeleteRecursively

@capability
trait DeleteRecursively:
  def conditionally[ResultType](path: Path)(operation: => ResultType): ResultType

object OverwritePreexisting:
  given default(using Quickstart, Tactic[OverwriteError]): OverwritePreexisting =
    filesystemOptions.doNotOverwritePreexisting

@capability
trait OverwritePreexisting:
  def apply[ResultType](path: Path)(operation: => ResultType): ResultType

object CreateNonexistentParents:
  given default(using Quickstart, Tactic[IoError]): CreateNonexistentParents =
    filesystemOptions.createNonexistentParents

@capability
trait CreateNonexistentParents:
  def apply[ResultType](path: Path)(operation: => ResultType): ResultType

object CreateNonexistent:
  given default(using Quickstart, Tactic[IoError]): CreateNonexistent =
    filesystemOptions.createNonexistent(using filesystemOptions.createNonexistentParents)

@capability
trait CreateNonexistent:
  def apply(path: Path)(operation: => Unit): Unit

object WriteSynchronously:
  given default(using Quickstart): WriteSynchronously = filesystemOptions.writeSynchronously

@capability
trait WriteSynchronously:
  def options(): List[jnf.StandardOpenOption]

package filesystemOptions:
  given dereferenceSymlinks: DereferenceSymlinks = new DereferenceSymlinks:
    def options(): List[jnf.LinkOption] = Nil

  given doNotDereferenceSymlinks: DereferenceSymlinks = new DereferenceSymlinks:
    def options(): List[jnf.LinkOption] = List(jnf.LinkOption.NOFOLLOW_LINKS)

  given moveAtomically: MoveAtomically with
    def options(): List[jnf.CopyOption] = List(jnf.StandardCopyOption.ATOMIC_MOVE)

  given doNotMoveAtomically: MoveAtomically with
    def options(): List[jnf.CopyOption] = Nil

  given copyAttributes: CopyAttributes with
    def options(): List[jnf.CopyOption] = List(jnf.StandardCopyOption.COPY_ATTRIBUTES)

  given doNotCopyAttributes: CopyAttributes with
    def options(): List[jnf.CopyOption] = Nil

  given deleteRecursively(using io: Tactic[IoError])
          : DeleteRecursively =

    new DeleteRecursively:
      def conditionally[ResultType](path: Path)(operation: => ResultType): ResultType =
        given symlinks: DereferenceSymlinks = doNotDereferenceSymlinks
        given creation: CreateNonexistent = doNotCreateNonexistent

        if path.exists() then
          if path.is[Directory] then path.as[Directory].children.each(conditionally(_)(()))
          jnf.Files.delete(path.stdlib)

        operation

  given doNotDeleteRecursively(using unemptyDirectory: Tactic[UnemptyDirectoryError])
          : DeleteRecursively =
    new DeleteRecursively:
      def conditionally[ResultType](path: Path)(operation: => ResultType): ResultType =
        try operation
        catch case error: jnf.DirectoryNotEmptyException => abort(UnemptyDirectoryError(path))

  given overwritePreexisting(using deleteRecursively: DeleteRecursively): OverwritePreexisting =
    new OverwritePreexisting:
      def apply[ResultType](path: Path)(operation: => ResultType): ResultType =
        deleteRecursively.conditionally(path)(operation)

  given doNotOverwritePreexisting(using overwrite: Tactic[OverwriteError]): OverwritePreexisting =
    new OverwritePreexisting:
      def apply[ResultType](path: Path)(operation: => ResultType): ResultType =
        try operation catch case error: jnf.FileAlreadyExistsException => abort(OverwriteError(path))

  given createNonexistentParents(using Tactic[IoError]): CreateNonexistentParents =
    new CreateNonexistentParents:
      def apply[ResultType](path: Path)(operation: => ResultType): ResultType =
        path.parent.let: parent =>
          given DereferenceSymlinks = filesystemOptions.doNotDereferenceSymlinks

          if !parent.exists() || !parent.is[Directory]
          then jnf.Files.createDirectories(parent.stdlib)

        operation

  given doNotCreateNonexistentParents(using io: Tactic[IoError]): CreateNonexistentParents =

    new CreateNonexistentParents:
      def apply[ResultType](path: Path)(operation: => ResultType): ResultType =
        try operation catch case error: ji.FileNotFoundException => abort(IoError(path))

  given createNonexistent(using createNonexistentParents: CreateNonexistentParents)
          : CreateNonexistent =
    new CreateNonexistent:
      def apply(path: Path)(operation: => Unit): Unit =
        if !path.exists() then createNonexistentParents(path)(operation)

  given doNotCreateNonexistent: CreateNonexistent = new CreateNonexistent:
    def apply(path: Path)(operation: => Unit): Unit = ()

  given writeSynchronously: WriteSynchronously with
    def options(): List[jnf.StandardOpenOption] = List(jnf.StandardOpenOption.SYNC)

  given doNotWriteSynchronously: WriteSynchronously with
    def options(): List[jnf.StandardOpenOption] = Nil

case class IoError(path: Path) extends Error(m"an I/O error occurred involving $path")

case class OverwriteError(path: Path)
extends Error(m"cannot overwrite a pre-existing filesystem node $path")

case class UnemptyDirectoryError(path: Path)
extends Error(m"the directory is not empty")

case class ForbiddenOperationError(path: Path)
extends Error(m"insufficient permissions to modify $path")

case class PathStatusError(path: Path)
extends Error(m"the filesystem node at $path was expected to be a different type")

case class SafeLink(ascent: Int, descent: List[Name[GeneralForbidden]]) extends Link

object SafeLink:
  given creator: PathCreator[SafeLink, GeneralForbidden, Int] = SafeLink(_, _)
  given SafeLink is Showable as show = _.render
  given encoder: Encoder[SafeLink] = _.render
  given SafeLink is Inspectable = _.render

  given (using PathCreator[SafeLink, GeneralForbidden, Int], ValueOf["."]) => SafeLink is Followable[GeneralForbidden, "..", "."] =
    new Followable[GeneralForbidden, "..", "."]:
      type Self = SafeLink
      val separators: Set[Char] = Set('/', '\\')
      def separator(link: SafeLink): Text = t"/"
      def ascent(link: SafeLink): Int = link.ascent
      def descent(link: SafeLink): List[Name[GeneralForbidden]] = link.descent

  inline given decoder(using Tactic[PathError]): Decoder[SafeLink] = Followable.decoder[SafeLink]

given (using log: IoEvent is Loggable) => ExecEvent is Loggable = log.contramap(IoEvent.Exec(_))

object IoEvent:
  given IoEvent is Communicable =
    case Exec(event) => event.communicate

enum IoEvent:
  case Exec(event: ExecEvent)
