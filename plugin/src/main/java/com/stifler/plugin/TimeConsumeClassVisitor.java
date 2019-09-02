package com.stifler.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;



public class TimeConsumeClassVisitor extends ClassVisitor {

  public TimeConsumeClassVisitor(ClassVisitor classVisitor) {
    super(Opcodes.ASM5, classVisitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                   String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv = new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

      private boolean inject = false;

      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (Type.getDescriptor(TimeConsume.class).equals(desc)) {
          System.out.println("===========visitAnnotation============="+ name);
          inject = true;
        }
        return super.visitAnnotation(desc, visible);
      }

      @Override
      protected void onMethodEnter() {
        if (inject) {
          System.out.println("======onMethodEnter===========begin======="+ name);
          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn("========start=========");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false);

          mv.visitLdcInsn(name);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
          mv.visitMethodInsn(INVOKESTATIC, "com/stifler/plugin/TimeCache", "setStartTime",
              "(Ljava/lang/String;J)V", false);
          System.out.println("======onMethodEnter===========end======="+ name);
        }
      }

      @Override
      protected void onMethodExit(int opcode) {
        if (inject) {
          System.out.println("======onMethodExit===========begin======="+ name);
          mv.visitLdcInsn(name);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
          mv.visitMethodInsn(INVOKESTATIC, "com/stifler/plugin/TimeCache", "setEndTime",
              "(Ljava/lang/String;J)V", false);

          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn(name);
          mv.visitMethodInsn(INVOKESTATIC, "com/stifler/plugin/TimeCache", "getCostTime",
              "(Ljava/lang/String;)Ljava/lang/String;", false);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false);

          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn("========end=========");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false);
          System.out.println("======onMethodExit===========end======="+ name);
        }
      }
    };
    return mv;
  }
}
