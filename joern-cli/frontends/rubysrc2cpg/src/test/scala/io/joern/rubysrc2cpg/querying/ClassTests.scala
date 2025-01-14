package io.joern.rubysrc2cpg.querying

import io.joern.rubysrc2cpg.testfixtures.RubyCode2CpgFixture
import io.joern.x2cpg.Defines
import io.shiftleft.codepropertygraph.generated.nodes.{Identifier, Return}
import io.shiftleft.semanticcpg.language.*

class ClassTests extends RubyCode2CpgFixture {

  "`class C ; end` is represented by an empty TYPE_DECL node" in {
    val cpg = code("""
                     |class C ; end
                     |""".stripMargin)

    val List(classC) = cpg.typeDecl.name("C").l

    classC.inheritsFromTypeFullName shouldBe List()
    classC.fullName shouldBe "Test0.rb:<global>::program.C"
    classC.lineNumber shouldBe Some(2)
    classC.baseType.l shouldBe List()
    classC.member.l shouldBe List()
    classC.method.name.l shouldBe List("<init>")
  }

  "`class C < D` is represented by a TYPE_DECL node inheriting from `D`" in {
    val cpg = code("""
                     |class C < D
                     |
                     |end
                     |""".stripMargin)

    val List(classC) = cpg.typeDecl.name("C").l

    classC.inheritsFromTypeFullName shouldBe List("D")
    classC.fullName shouldBe "Test0.rb:<global>::program.C"
    classC.lineNumber shouldBe Some(2)
    classC.member.l shouldBe List()
    classC.method.name.l shouldBe List("<init>")

    val List(typeD) = classC.baseType.l
    typeD.name shouldBe "D"
  }

  "`attr_reader :a` is represented by a `@a` MEMBER node" in {
    val cpg = code("""
                     |class C
                     | attr_reader :a
                     |end
                     |""".stripMargin)

    val List(classC)  = cpg.typeDecl.name("C").l
    val List(aMember) = classC.member.l

    aMember.name shouldBe "@a"
    aMember.code shouldBe "attr_reader :a"
    aMember.lineNumber shouldBe Some(3)
  }

  "`attr_reader :'abc'` is represented by a `@abc` MEMBER node" in {
    val cpg = code("""
                     |class C
                     | attr_reader :'abc'
                     |end
                     |""".stripMargin)

    val List(classC)    = cpg.typeDecl.name("C").l
    val List(abcMember) = classC.member.name("@abc").l

    abcMember.code shouldBe "attr_reader :'abc'"
    abcMember.lineNumber shouldBe Some(3)
  }

  "`attr_reader :'abc' creates an `abc` METHOD node" in {
    val cpg = code("""
                     |class C
                     | attr_reader :'abc'
                     |end
                     |""".stripMargin)

    val List(classC)    = cpg.typeDecl.name("C").l
    val List(methodAbc) = classC.method.name("abc").l

    methodAbc.code shouldBe "def abc (...)"
    methodAbc.lineNumber shouldBe Some(3)
    methodAbc.parameter.isEmpty shouldBe true
    methodAbc.fullName shouldBe "Test0.rb:<global>::program.C:abc"

    // TODO: Make sure that @abc in this return is the actual field
    val List(ret: Return)          = methodAbc.methodReturn.cfgIn.l: @unchecked
    val List(abcField: Identifier) = ret.astChildren.l: @unchecked
    ret.code shouldBe "return @abc"
    abcField.name shouldBe "@abc"
  }

  "`attr_reader :a, :b` is represented by `@a`, `@b` MEMBER nodes" in {
    val cpg = code("""
                     |class C
                     | attr_reader :a, :b
                     |end""".stripMargin)

    val List(classC)  = cpg.typeDecl.name("C").l
    val List(aMember) = classC.member.name("@a").l
    val List(bMember) = classC.member.name("@b").l

    aMember.code shouldBe bMember.code
    aMember.lineNumber shouldBe bMember.lineNumber
  }

  "`attr_writer :a` is represented by a `@a` MEMBER node" in {
    val cpg = code("""
                     |class C
                     | attr_writer :a
                     |end
                     |""".stripMargin)

    val List(classC)  = cpg.typeDecl.name("C").l
    val List(aMember) = classC.member.name("@a").l

    aMember.code shouldBe "attr_writer :a"
    aMember.lineNumber shouldBe Some(3)
  }

  "`attr_writer :a` creates an 'a=' METHOD node" in {
    val cpg = code("""
                     |class C
                     | attr_writer :a
                     |end
                     |""".stripMargin)

    val List(classC)  = cpg.typeDecl.name("C").l
    val List(methodA) = classC.method.name("a=").l

    methodA.code shouldBe "def a= (...)"
    methodA.lineNumber shouldBe Some(3)
    methodA.fullName shouldBe "Test0.rb:<global>::program.C:a="

    // TODO: there's probably a better way for testing this
    val List(param)                            = methodA.parameter.l
    val List(assignment)                       = methodA.assignment.l
    val List(lhs: Identifier, rhs: Identifier) = assignment.argument.l: @unchecked

    param.name shouldBe rhs.name
    lhs.name shouldBe "@a"
  }

  "`attr_accessor :a` is represented by a `@a` MEMBER node" in {
    val cpg = code("""
                     |class C
                     | attr_accessor :a
                     |end
                     |""".stripMargin)

    val List(classC)  = cpg.typeDecl.name("C").l
    val List(aMember) = classC.member.name("@a").l

    aMember.code shouldBe "attr_accessor :a"
    aMember.lineNumber shouldBe Some(3)
  }

  "`def f(x) ... end` is represented by a METHOD inside the TYPE_DECL node" in {
    val cpg = code("""
                     |class C
                     | def f(x)
                     |   x + 1
                     | end
                     |end
                     |""".stripMargin)

    val List(classC)  = cpg.typeDecl.name("C").l
    val List(methodF) = classC.method.name("f").l

    methodF.fullName shouldBe "Test0.rb:<global>::program.C:f"
  }

  "`def initialize() ... end` directly inside a class has method name `<init>`" in {
    val cpg = code("""
                     |class C
                     | def initialize()
                     | end
                     |end
                     |""".stripMargin)

    val List(classC)     = cpg.typeDecl.name("C").l
    val List(methodInit) = classC.method.name("<init>").l

    methodInit.fullName shouldBe "Test0.rb:<global>::program.C:<init>"
  }

  "`class C end` has default constructor" in {
    val cpg = code("""
                     |class C
                     |end
                     |""".stripMargin)

    val List(classC)     = cpg.typeDecl.name("C").l
    val List(methodInit) = classC.method.name("<init>").l

    methodInit.fullName shouldBe "Test0.rb:<global>::program.C:<init>"
  }

  "`def initialize() ... end` not directly under class has method name `initialize`" in {
    val cpg = code("""
                     |def initialize()
                     |  1
                     |end
                     |
                     |class C
                     | def f()
                     |   2
                     | end
                     |end
                     |
                     |class D
                     | def f()
                     |   def initialize()
                     |     3
                     |   end
                     | end
                     |end
                     |""".stripMargin)

    cpg.method.name("<init>").literal.code.l should be(empty)
  }

  "a basic anonymous class" should {
    val cpg = code("""
        |a = Class.new do
        |  def hello
        |    puts "Hello world!"
        |  end
        |end
        |""".stripMargin)

    "generate a type decl with the associated members" in {
      inside(cpg.typeDecl.nameExact("<anon-class-0>").l) {
        case anonClass :: Nil =>
          anonClass.name shouldBe "<anon-class-0>"
          anonClass.fullName shouldBe "Test0.rb:<global>::program.<anon-class-0>"
          inside(anonClass.method.l) {
            case defaultConstructor :: hello :: Nil =>
              defaultConstructor.name shouldBe Defines.ConstructorMethodName
              defaultConstructor.fullName shouldBe s"Test0.rb:<global>::program.<anon-class-0>:${Defines.ConstructorMethodName}"

              hello.name shouldBe "hello"
              hello.fullName shouldBe "Test0.rb:<global>::program.<anon-class-0>:hello"
            case xs => fail(s"Expected a single method, but got [${xs.map(x => x.label -> x.code).mkString(",")}]")
          }
        case xs => fail(s"Expected a single anonymous class, but got [${xs.map(x => x.label -> x.code).mkString(",")}]")
      }
    }

    "generate an assignment to the variable `a` with the source being a constructor invocation of the class" in {
      inside(cpg.method(":program").assignment.l) {
        case aAssignment :: Nil =>
          aAssignment.target.code shouldBe "a"
          // TODO: Constructors are not supported, we simply check the `code` property
          aAssignment.source.code shouldBe "Class.new"
        case xs => fail(s"Expected a single assignment, but got [${xs.map(x => x.label -> x.code).mkString(",")}]")
      }
    }

  }

  "a basic singleton class" should {
    val cpg = code("""class Animal; end
        |animal = Animal.new
        |
        |class << animal
        |  def bark
        |    'Woof'
        |  end
        |end
        |
        |animal.bark # => 'Woof'
        |""".stripMargin)

    "generate a type decl with the associated members" in {
      inside(cpg.typeDecl.nameExact("<anon-class-0>").l) {
        case anonClass :: Nil =>
          anonClass.name shouldBe "<anon-class-0>"
          anonClass.fullName shouldBe "Test0.rb:<global>::program.<anon-class-0>"
          // TODO: Attempt to resolve the below with the `scope` class once we're handling constructors
          anonClass.inheritsFromTypeFullName shouldBe Seq("animal")
          inside(anonClass.method.l) {
            case defaultConstructor :: bark :: Nil =>
              defaultConstructor.name shouldBe Defines.ConstructorMethodName
              defaultConstructor.fullName shouldBe s"Test0.rb:<global>::program.<anon-class-0>:${Defines.ConstructorMethodName}"

              bark.name shouldBe "bark"
              bark.fullName shouldBe "Test0.rb:<global>::program.<anon-class-0>:bark"
            case xs => fail(s"Expected a single method, but got [${xs.map(x => x.label -> x.code).mkString(",")}]")
          }
        case xs => fail(s"Expected a single anonymous class, but got [${xs.map(x => x.label -> x.code).mkString(",")}]")
      }
    }

    "register that `animal` may possibly be an instantiation of the singleton type" in {
      cpg.local("animal").possibleTypes.l should contain("Test0.rb:<global>::program.<anon-class-0>")
    }

  }

}
