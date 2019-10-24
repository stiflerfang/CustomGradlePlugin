package com.stifler.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;


public class MethodStatisticsClassVisitor extends ClassVisitor {
  private static int sMethodMount = 0;

  public MethodStatisticsClassVisitor(ClassVisitor classVisitor) {
    super(Opcodes.ASM5, classVisitor);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                   String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    System.out.println("===========visitMethod=============" + name);
    if (name.contains("access")) {
      System.out.println("===========sMethodMount=============" + ++sMethodMount);
    }
    mv = new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {


      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
      }

      @Override
      protected void onMethodEnter() {
      }

      @Override
      protected void onMethodExit(int opcode) {
      }
    };
    return mv;
  }


}
