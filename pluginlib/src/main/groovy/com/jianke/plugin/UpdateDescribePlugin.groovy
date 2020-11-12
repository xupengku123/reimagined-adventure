package com.jianke.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * @author zy* @date 2020/11/4 18:13
 * @version 1.0* @description
 * @modified
 */
class UpdateDescribePlugin implements Plugin<Project> {

    def historyFile = new File("versiondescribe.xml")
/**
 * 获取一个 GPathResult 对象
 * @see groovy.util.slurpersupport.GPathResult*
 * */
    static def openXml(xml) {
        def xmlSlurper = new XmlSlurper()
        def result = xmlSlurper.parse(xml)
        return result
    }

    static def openXmlText(xmlText) {
        def xmlSlurper = new XmlSlurper()
        def result = xmlSlurper.parseText(xmlText)
        return result
    }
/**
 * 当文件不存在时初始化文件
 * @param file 固定的文件
 * @return
 */
    def initVersionXml(file) {
        file.createNewFile()
        println file.exists()
        file.withWriter('utf-8') {
            writer ->
                writer.writeLine '<?xml version="1.0" encoding="utf-8"?>\n'
                writer.writeLine '<history>'
                writer.writeLine '</history>'
        }
    }

/**
 * 新增一个版本结点
 * @param pathResult 父结点history
 * @param versionCode 版本号
 * @param versionName 版本名称
 * @param content 更新内容
 * @param describe 更新描述
 * @return
 */
    static def addNewVersion(pathResult, versionCode, versionName, content, describe) {
        def xmlText = groovy.xml.XmlUtil.serialize(pathResult)
        def xmlParser = new groovy.util.XmlParser()
        def rootNode = xmlParser.parseText(xmlText)
        def targetNode = null
        rootNode.depthFirst().each {
            if (it.name() == "history") {
                targetNode = it
            }
        }
        def versionNode = new groovy.util.Node(null, "version", ["versionCode": versionCode, "versionName": versionName])
        versionNode.append(new groovy.util.Node(null, "content", content))
        versionNode.append(new groovy.util.Node(null, "describe", describe))
        targetNode?.append(versionNode)
        def afterXmlText = groovy.xml.XmlUtil.serialize(rootNode)
        return openXmlText(afterXmlText)
    }

    static def getValue(pathResult, key) {
        return getNodeChildren(pathResult, key)[0].text()
    }

    /**
     * 获取属性名为 name 值为 key 的 Node
     * */
    static def getNodeChildren(pathResult, key) {
        pathResult?.depthFirst()?.findAll { node ->
            if (node["@name"] == key) {
                return node
            }
        }
    }

    /**
     * @return 返回修改之前的值*            */
    static def setValue(pathResult, key, value) {
        def nodeChildren = getNodeChildren(pathResult, key)
        println pathResult
        println pathResult.name
        println pathResult[0]
        println nodeChildren[0]
//        def oldValue = nodeChildren[0].text()
//        nodeChildren[0].replaceBody(value)

        return null
    }

/**
 * 保存修改
 * @param targetFile 目标文件
 * @param xml 父结点
 * @return
 */
    static def save(targetFile, xml) {
        def text = null

        if (xml instanceof groovy.util.slurpersupport.GPathResult) {
            text = groovy.xml.XmlUtil.serialize(xml)
        } else {
            text = xml.toString()
        }

        def tempFile = null
        if (targetFile instanceof File) {
            tempFile = targetFile
        } else {
            tempFile = new File(targetFile)
        }

        tempFile.withWriter("UTF8") { writer ->
            writer.write(text)
        }
    }

    static def logXmlText(node) {
        def xmlText = groovy.xml.XmlUtil.serialize(node)
        println xmlText
    }

    @Override
    void apply(Project project) {
        project.extensions.create('versionDescribeConfig', UpdateDescribeExtension);

        project.task('versionDescribe') {
            doLast {
                //只可以在 android application 或者 android lib 项目中使用
                if (!project.android) {
                    throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
                }
                if (!historyFile.exists()) {
                    initVersionXml(historyFile)
                }
                println historyFile.absolutePath
                def versionCode = project.android.defaultConfig.versionCode
                def versionName = project.android.defaultConfig.versionName
                def updateContent = project['versionDescribeConfig'].updateContent
                def updateDescribe = project['versionDescribeConfig'].updateDescribe
                def willCover = project['versionDescribeConfig'].willCover
                def historyNode = openXml(historyFile)
                def canAdd = true
                def modifiedNode = null
                historyNode.depthFirst().each {
                    if (it.name() == "version") {
                        if (it["@versionCode"] == versionCode) {
                            println "exist"
                            canAdd = false
                            modifiedNode = it
                        }
                    }
                }
                if (canAdd) {
                    def historyResult = addNewVersion(historyNode, versionCode, versionName, updateContent, updateDescribe)
                    save(historyFile, historyResult)
                } else {
                    if (willCover) {
                        modifiedNode["content"].replaceBody(updateContent)
                        modifiedNode["describe"].replaceBody(updateDescribe)
                        save(historyFile, historyNode)
                    }
                }
            }

        }
    }
}
