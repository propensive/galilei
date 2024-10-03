package galilei

import prepositional.*
import turbulence.*
import rudiments.*
import serpentine.*
import contingency.*
import anticipation.*

import java.nio.channels as jnc
import java.nio.file as jnf

object Openable:
  given [PlatformType <: Filesystem]
      (using read:        ReadAccess,
             write:       WriteAccess,
             dereference: DereferenceSymlinks,
             create:      CreateNonexistent on PlatformType,
             streamError: Tactic[StreamError],
             ioError:     Tactic[IoError])
      => (Path on PlatformType) is Openable by jnf.OpenOption into Handle = new Openable:

    type Self = Path on PlatformType
    type Operand = jnf.OpenOption
    type Result = Handle
    protected type Carrier = jnc.FileChannel
  
    def init(path: Path on PlatformType, extraOptions: List[jnf.OpenOption]): jnc.FileChannel =
      val options = read.options() ++ write.options() ++ dereference.options() ++ create.options() ++
          extraOptions

      path.protect(IoError.Operation.Open)(jnc.FileChannel.open(path.javaPath, options*).nn)
    
    def handle(channel: jnc.FileChannel): Handle =
      Handle
       (() => Readable.channel.stream(channel).stream[Bytes],
        Writable.channel.write(channel, _))
        

    def close(channel: jnc.FileChannel): Unit = channel.close()

trait Openable:
  type Self
  type Operand
  type Result
  protected type Carrier

  def init(value: Self, options: List[Operand]): Carrier
  def handle(carrier: Carrier): Result

  def open[ResultType](value: Self, lambda: Result => ResultType, options: List[Operand])
          : ResultType =
    val carrier = init(value, options)
    try lambda(handle(carrier)) finally close(carrier)

  def close(carrier: Carrier): Unit