package com.stifler.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

public class MethodStatisticsPlugin extends Transform implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        println '========================'
        println 'hello gradle plugin!'
        println '========================'
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return "stifler"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println '//===============asm visit start===============//'

        def startTime = System.currentTimeMillis()

        inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput directoryInput ->

                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        def name = file.name
                        if (name.endsWith(".class") && !name.startsWith("R\$") &&
                                !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {

                            println name + ' is changing...'

                            ClassReader cr = new ClassReader(file.bytes)
                            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                            ClassVisitor cv = new MethodStatisticsClassVisitor(cw)

                            cr.accept(cv, ClassReader.EXPAND_FRAMES)

                            byte[] code = cw.toByteArray()
                            File destFile = new File(file.parentFile.absolutePath + File.separator + name)
                            println '==== 重新写入的位置->lastFilePath === ' + destFile.getAbsolutePath()
                            FileOutputStream fos = new FileOutputStream(destFile)
                            fos.write(code)
                            fos.close()
                        }
                    }
                }

                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)


                FileUtils.copyDirectory(directoryInput.file, dest)
            }

//            input.jarInputs.each { JarInput jarInput ->
//                def jarName = jarInput.name
//                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
//                if (jarName.endsWith(".jar")) {
//                    jarName = jarName.substring(0, jarName.length() - 4)
//                }
//
//                def dest = outputProvider.getContentLocation(jarName + md5Name,
//                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
//
//                FileUtils.copyFile(jarInput.file, dest)
//            }

            input.jarInputs.each { JarInput jarInput ->
//                def jarName = jarInput.name
//                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
//                if (jarName.endsWith(".jar")) {
//                    jarName = jarName.substring(0, jarName.length() - 4)
//                }

                File tmpFile = null
                if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
                    JarFile jarFile = new JarFile(jarInput.file)
                    Enumeration enumeration = jarFile.entries()
                    tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_trace.jar")
                    if (tmpFile.exists()) {
                        tmpFile.delete()
                    }
                    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
                    ArrayList<String> processorList = new ArrayList<>()
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                        String entryName = jarEntry.getName()
                        ZipEntry zipEntry = new ZipEntry(entryName)
                        InputStream inputStream = jarFile.getInputStream(jarEntry)
                        if (entryName.endsWith(".class") && !entryName.contains("R\$") &&
                                !entryName.contains("R.class") && !entryName.contains("BuildConfig.class")) {
                            jarOutputStream.putNextEntry(zipEntry)
                            ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                            ClassVisitor cv = MethodStatisticsClassVisitor(classWriter)
                            classReader.accept(cv, EXPAND_FRAMES)
                            byte[] code = classWriter.toByteArray()
                            jarOutputStream.write(code)

                        } else if (entryName.contains("META-INF/services/javax.annotation.processing.Processor")) {
                            if (!processorList.contains(entryName)) {
                                processorList.add(entryName)
                                jarOutputStream.putNextEntry(zipEntry)
                                jarOutputStream.write(IOUtils.toByteArray(inputStream))
                            } else {
                                println "duplicate entry:" + entryName
                            }
                        } else {
                            jarOutputStream.putNextEntry(zipEntry)
                            jarOutputStream.write(IOUtils.toByteArray(inputStream))
                        }

                        jarOutputStream.closeEntry()
                    }
                    jarOutputStream.close()
                    jarFile.close()
                }
                def dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (tmpFile == null) {
                    FileUtils.copyFile(jarInput.file, dest)
                } else {
                    FileUtils.copyFile(tmpFile, dest)
                    tmpFile.delete()
                }
            }
        }

        def cost = (System.currentTimeMillis() - startTime) / 1000

        println "plugin cost $cost secs"
        println '//===============asm visit end===============//'
    }
}
