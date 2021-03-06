package jp.gr.java_conf.harada;
import java.io._
import scala.io.Source
import javax.script._
import scala.sys.process._
import scala.collection.convert.WrapAsScala._

object FilesOp {
  def main(args: Array[String]) {
    var verbose = false;
    var encoding : String = null;
    var pathstarts : List[String] = Nil;
    var excludestarts : List[String] = Nil;
    var pathends : List[String] = Nil;
    var notonlyfile = false;
    var n = args.size;
    var skip = 0;
    for (i<-0 until args.size) {
      if (skip > 0) skip -= 1 else if (skip == 0) {
        if (args(i).charAt(0) != '-') {
          skip = -1;
          n = i;
        } else args(i) match {
          case "-verbose" => verbose = true;
          case "-pathstarts" => 
            skip = 1;
            pathstarts ++= args(i+1).split(",").toList;
          case "-excludestarts" => 
            skip = 1;
            excludestarts ++= args(i+1).split(",").toList;
          case "-pathends" => 
            skip = 1;
            pathends ++= args(i+1).split(",").toList;
          case "-notonlyfile" => notonlyfile = true;
          case _ => println("unknown option : "+ args(i));
        }
      }
    }

    if (n+1 >= args.length) {
      System.out.println("FilesOp (options) [basedir] [command].. : it operates files under [basedir] for [command].");
      System.out.println("(options) ");
      System.out.println(" -pathstarts [pre1,pre2,..] : relative paths of target file must start with pre1, pre2, and so on.");
      System.out.println(" -excludestarts [pre1,pre2,..] : relative paths of target file must not start with pre1, pre2, and so on.");
      System.out.println(" -pathends [ext1,ext2,..] : relative paths of target file must end with pre1, pre2, and so on.");
      System.out.println(" -notonlyfile : target is not only file but also directory.");
      System.out.println("[command]");
      System.out.println(" path : show paths of target file.");
      System.out.println(" addcr (addcropt) : modify line ends to System.getProperty(\"line.separator\").");
      System.out.println("  (addcropt)");
      System.out.println("   -encoding [enc] : specify the encoding it reads target files with.");
      System.out.println("   -outencoding [enc] : specify the encoding it writes resulting files with.");
      System.out.println("   -lf : modify line end to \\n");
      System.out.println("   -crlf : modify line end to \\r\\n");
      System.out.println(" rmbom :  remove byte order mark of UTF-8.");
      System.out.println(" remove : delete target files. use -notonlyfile to remove directory.");
      System.out.println(" copy [todir] : copy target files to [todir].");
      System.out.println(" command (commandopt) [commandfunc] : run command for each file.(parameter is _)");
      System.out.println("   ex. FilesOp src command \"find \\\"searchstring\\\" _\" ");
      System.out.println(" findjar [name] : find jar which contains class whose name ends with [name].");
      System.out.println("   this command specifies for default the option :-pathends .jar.");
      System.exit(1);
    }
    try {
      val filter : (File, String)=>Boolean = if (pathends == Nil) (File,String)=>true else {
        (f:File, path:String) => if (pathends.find(path.endsWith(_))==None) false else true;
      }
      val fop = new FilesOp(filter, notonlyfile, pathstarts, excludestarts);
      fop.verbose = verbose;
      val basedir = new File(args(n));
      val basepath = if (basedir.isFile) basedir.getName else "";

      args(n+1) match {
        case "path" => fop.op((f:File,path:String)=>println(path), basedir, basepath);
        case "addcr" => 
          var lf = System.getProperty("line.separator");
          var encoding : String = null;
          var outencoding : String = null;
          skip = 0;
          var m = args.size;
          for (i<-(n+2) until args.size if m == args.size) {
            if (skip > 0) skip -= 1 else if (args(i).charAt(0) != '-') {
              m = i;
            } else args(i) match {
              case "-lf" => lf = "\n";
              case "-crlf" => lf = "\r\n";
              case "-encoding" => skip = 1; encoding = args(i+1);
              case "-outencoding" => skip = 1; outencoding = args(i+1);
              case _ => println("unknown addcr option : " + args(i));
            }
          }
          fop.op((f:File,path:String)=>addcr(f, lf, encoding, outencoding), basedir, basepath);
        case "rmbom" => fop.op((f:File,path:String)=>rmbom(f), basedir, basepath);
        case "remove" => fop.op((f:File,path:String)=>f.delete, basedir, basepath);
        case "copy" => 
          val todir = new File(args(n+2));
          val topath = todir.getCanonicalPath;
          val path = basedir.getCanonicalPath;
          if (topath.startsWith(path) || path.startsWith(topath)) throw new IllegalArgumentException("It cannot copy to/from it's subdirectory.");
          fop.op((f:File,path:String)=>copyfile(f, new File(todir, path)), basedir, basepath);
        case "command" => 
          def getcommand(f:File) = args(n+2).replace("_", f.getPath);

          fop.op((f:File,path:String)=>{
            getcommand(f)!
          }, basedir, basepath);

        case "findjar" =>
          if (pathends == Nil) {
            val prefil = fop.filter;
            fop.filter = (f:File,path:String)=>if (path.endsWith(".jar")) prefil(f,path) else false;
          }
          fop.op((f:File,path:String)=>findjar(f,path, args(n+2)), basedir, basepath);
      }
    } catch {
      case e: Exception => e.printStackTrace(System.out);
    }
  }

  def addcr(f:File, lf:String, encoding:String = null, outencoding:String=null) {
    val source = if (encoding == null) Source.fromFile(f) else Source.fromFile(f, encoding);
    var sb = new StringBuilder;
    var change = false;
    var lines : List[String] = Nil;
    for (c<-source) {
      c match {
        case '\n' => 
          var s = sb.toString;
          sb = new StringBuilder;
          if (s.endsWith("\r")) {
            if (!lf.startsWith("\r")) {
              change = true;
              s = s.substring(0, s.length-1);
            }
          } else if (lf.startsWith("\r")) {
            change = true;
          }
          lines :+= (s + lf);
        case _ => sb.append(c);
      }		 
    }
    source.close;
    lines :+= sb.toString;

    if (change || (outencoding != null)) {
println("[addcr] " + f);
      val enc = if (outencoding != null) outencoding else encoding;
      val wr = if (enc == null) new FileWriter(f) else new OutputStreamWriter(new FileOutputStream(f), enc);
      for (line<-lines) wr.write(line);
      wr.close;
    }
  }
  lazy val buffer = new Array[Byte](0x100000);
  def rmbom(f:File) {
    val boms = Array(0xEF, 0xBB, 0xBF);
    val ins = new FileInputStream(f);
    for (check<-boms) {
      if (ins.read() != check) {
        ins.close;
        return;
      }
    }
    var bous = new ByteArrayOutputStream;
    var end = false;
    while (!end) {
      val n = ins.read(buffer);
      if (n == -1) end = true else {
        bous.write(buffer, 0, n);
      }
    }
    ins.close;
println("[rmbom] " + f);
    val ous = new FileOutputStream(f);
    ous.write(bous.toByteArray);
    ous.close;
  }
  def copyfile(f:File, to:File) {
    if (to.getParentFile != null) to.getParentFile.mkdirs;
    val ins = new FileInputStream(f);
    val ous = new FileOutputStream(to);
    var end = false;
    while (!end) {
      val n = ins.read(buffer);
      if (n == -1) end = true else {
        ous.write(buffer, 0, n);
      }
    }
println("[copy] " + f + " => " + to);
    ins.close;
    ous.close;
    to.setLastModified(f.lastModified);
  }
import java.util.jar._;
  def findjar(f:File, path:String, name:String) {
    val jf = new JarFile(f);
    for (entry<-jf.entries) {
      if (entry.getName.endsWith(name + ".class")) {
println("[findjar]" + path + " (" + entry.getName + ")");
        return;
      }
    }
  }
}

class FilesOp(var filter:(File, String)=>Boolean, notonlyfile:Boolean = false, pathstarts:List[String]=null, excludestarts:List[String]=Nil) {
  var verbose = false;
  def op(func:(File, String)=>Unit, target:File, path:String = "") {
    if ((pathstarts != Nil) && (pathstarts.find((s:String)=>(path.startsWith(s) || s.startsWith(path))) == None)) return;
    if (excludestarts.find((s:String)=>(path.startsWith(s))) != None) return;

    if (target.isDirectory) {
      val p = if (path.isEmpty) "" else (path + "/");
      for (child<-target.listFiles) {
        op(func, child, p + child.getName);
      }
    }
    if (notonlyfile || target.isFile) {
      if (filter(target, path)) {
if (verbose) println("[operate]" + path);
        func(target, path);
      }
    }

  }
}
