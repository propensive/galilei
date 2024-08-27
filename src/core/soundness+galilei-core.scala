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

package soundness

export galilei.{BlockDevice, CharDevice, CopyAttributes, CreateNonexistent,
    CreateNonexistentParents, DeleteRecursively, DereferenceSymlinks, Directory, Entry, EntryMaker,
    Fifo, File, ForbiddenOperationError, IoError, Relative, MoveAtomically, OverwriteError,
    OverwritePreexisting, Path, PathResolver, PathStatus, PathStatusError, SafeRelative, Socket,
    Symlink, UnemptyDirectoryError, Unix, Volume, Windows, WriteSynchronously, GeneralForbidden}

package filesystemOptions:
  export galilei.filesystemOptions.{dereferenceSymlinks, doNotDereferenceSymlinks,
      moveAtomically, doNotMoveAtomically, copyAttributes, doNotCopyAttributes, deleteRecursively,
      doNotDeleteRecursively, overwritePreexisting, doNotOverwritePreexisting,
      createNonexistentParents, doNotCreateNonexistentParents, createNonexistent,
      doNotCreateNonexistent, writeSynchronously, doNotWriteSynchronously}

package pathHierarchies:
  export serpentine.pathHierarchies.{windows, unix, unixOrWindows}