@Grab(group='org.codehaus.groovy', module='groovy-all', version='2.4.8')

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ClosureExpression

import groovy.json.JsonSlurper

def repo_base = '../../../../'

class ScriptPathVisitor extends CodeVisitorSupport {
    def repo = ''
    def path = ''

    void visitMethodCallExpression(MethodCallExpression node) {
        def method = node.method.getText()
        if (method == 'github') {
            repo = node.arguments.expressions[0].getText()
        }
        if (method == "scriptPath") {
            path = node.arguments.expressions[0].getText()
        }
        node.getArguments().visit(this)
    }
}

class FolderVisitor extends CodeVisitorSupport {
    Set usedDirs = []
    Set createdDirs = []

    void addToUsedDirs(String dir) {
        def parts = dir.split('/')
        def created = []
        def prefix = ''
        parts[0..parts.length-2].each {
            def tocreate = created.join('/') + prefix + it
            created << it
            usedDirs << tocreate
            prefix = '/'
        }
    }

    void visitMethodCallExpression(MethodCallExpression node) {
        if ("pipelineJob" == node.method.getText() ||
            "multibranchPipelineJob" == node.method.getText()) {
            if (node.arguments.expressions[0] instanceof ConstantExpression) {
                def dir = node.arguments.expressions[0].getText()
                addToUsedDirs(dir)
            }
        }
        if("folder" == node.method.getText()) {
            def created = node.arguments.expressions[0].getText()
            createdDirs << created
        }
        node.getArguments().visit(this)
    }
}

String[] jobdslFiles = new FileNameFinder().getFileNames(repo_base + '/Jenkins/Pipelines', '**/*.groovy')
String foldersFile = new File(repo_base + 'Jenkins/Pipelines/Folders.groovy').text
FolderVisitor visitor = new FolderVisitor()
ScriptPathVisitor scriptVisitor = new ScriptPathVisitor()
Set invalidScriptPaths = []

jobdslFiles.each {
    def src = new File(it).text
    def ast = new AstBuilder().buildFromString(CompilePhase.SEMANTIC_ANALYSIS, true, src)[0]
    ast.visit(visitor)
    ast.visit(scriptVisitor)
    if (scriptVisitor.path != '' && scriptVisitor.repo == 'cbdr/CloudOps' && !new File(repo_base + scriptVisitor.path).exists()) {
        invalidScriptPaths << scriptVisitor.path
    }
}
def ast = new AstBuilder().buildFromString(CompilePhase.SEMANTIC_ANALYSIS, true, foldersFile)[0]
ast.visit(visitor)

def slurper = new groovy.json.JsonSlurper()
slurper.parse(new FileReader(new File(repo_base + 'Jenkins/Config/CI.json'))).each {
    visitor.addToUsedDirs(it.getKey())
}

usedButNotCreated = visitor.usedDirs - visitor.createdDirs
createdButNotUsed = (visitor.createdDirs - visitor.usedDirs).findAll { it.contains('/') }
println "Directories used but not created: ${usedButNotCreated}"
println "Directories created but not used: ${createdButNotUsed}"
println "ScriptPaths used, but whose scripts do not exist: ${invalidScriptPaths}"

if (args.length > 0 && args[0] == '--auto-fix') {
    File file = new File(repo_base + 'Jenkins/Pipelines/Folders.groovy')
    lastByte = file.getBytes()[file.getBytes().length - 1]
    if (lastByte != (int)'\n') {
        println "Appending newline to Folders.groovy"
        file.append("\n")
    }
    usedButNotCreated.each {
        println "Appending ${it} to Folders.groovy"
        file.append("folder('${it}')\n")
    }
    blocks = []
    curblock = []
    file.readLines().each {
      if (it.startsWith('folder(')) {
        blocks << curblock
        curblock = []
      }
      curblock << it
    }
    blocks << curblock
    blocks.sort { it[0] }
    file.setText("")

    blocks.each { block ->
      block.each { line ->
        if(line != "") {
          file.append(line + "\n")
        }
      }
    }
    System.exit(0)
}

if (usedButNotCreated.size() > 0 || createdButNotUsed.size() > 0) {
    System.exit(1)
}

if (invalidScriptPaths.size() > 0) {
    System.exit(1)
}
