package graphics.animation

import maths._
import org.lwjgl.assimp._
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIFace
import org.lwjgl.assimp.AIMatrix4x4
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIVector3D
import org.lwjgl.assimp.AIVertexWeight
import org.lwjgl.assimp.Assimp
import org.lwjgl.assimp.AINodeAnim
import org.lwjgl.assimp.AIVectorKey
import org.lwjgl.assimp.AIQuatKey

import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL14._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL42._
import org.lwjgl.opengl.GL43._
import org.lwjgl.opengl.GL45._

import org.lwjgl.system.MemoryStack._
import org.lwjgl.system.MemoryUtil._
import org.lwjgl.system._

class AnimatedModel(private val animationLibrary: Map[String, Animation], private val rootJoint: Joint,
                    private val joints: Array[Joint], val modelVao: Int, val vertexCount: Int) {

    private var lastPose: Pose = Pose(joints map (_.transform))
    private var currentAnimation: Animation = animationLibrary.head._2
    private var animationChangeFlag = false

    def setAnimation(name: String) = {
        animationLibrary get name match {
            case Some(animation) => animationChangeFlag = true; currentAnimation = animation
            case None => println("animation not found in the library")
        }
    }

    def update(dt: Double): Unit = {
        advanceAnimation(dt)
        val pose = currentAnimation.currentPose
        applyPoseToModel(pose)
    }

    def advanceAnimation(dt: Double): Unit = currentAnimation.advance(dt)

    def jointTransforms: Array[Matrix4] = joints map (_.animationTransform)

    private def applyPoseToModel(pose: Pose) = applyPoseToJoints(pose, rootJoint, Matrix4.identity)

    private def applyPoseToJoints(pose: Pose, joint: Joint, parentTransform: Matrix4): Unit = {
        joint.transform = pose.jointTransforms(joint.id)
        val currentTransform = parentTransform * joint.localTransform
        for (j <- joint.children)
            applyPoseToJoints(pose, j, currentTransform)
        joint.animationTransform = currentTransform * joint.inverseBindTransform
    }


}

object AnimatedModel {
    def load(path: String): AnimatedModel = {
        val scene = Assimp.aiImportFile(path, Assimp.aiProcess_LimitBoneWeights)



        if (scene == null || scene.mNumAnimations() == 0) {
            println("no animations found in the imported file")
        }

        val mesh = AIMesh.create(scene.mMeshes().get(0))
        println("created mesh")
        println("number of meshes" + scene.mNumMeshes())
        println("number of materials" + scene.mNumMaterials())
        println("number of animations" + scene.mNumAnimations())
        println("number of bones " + mesh.mNumBones())
        println("number of vertices " + mesh.mNumVertices())
        println("number of faces " + mesh.mNumFaces())
        val indicesBuffer = MemoryUtil.memAllocInt(mesh.mNumFaces() * 3)

        val vertexCount = mesh.mNumFaces() * 3

        for (i <- 0 until mesh.mNumFaces()) {
            val face = mesh.mFaces().get(i)
            for (j <- 0 until 3)
                indicesBuffer.put(face.mIndices().get(j))
        }
        indicesBuffer.flip()


        val weightsArray = Array.fill[List[(Int, Float)]](mesh.mNumVertices())(Nil)


        val jointNamesMap = scala.collection.mutable.HashMap[String, Int]()

        for (i <- 0 until mesh.mNumBones()) {
            val bone = AIBone.create(mesh.mBones().get(i))
            jointNamesMap += (bone.mName().dataString() -> i)
            println(i + " " + bone.mName().dataString)
            for (j <- 0 until bone.mNumWeights()) {
                val weight = bone.mWeights().get(j)
                val vertexIndex = weight.mVertexId()
                weightsArray(vertexIndex) = (i, weight.mWeight()) :: weightsArray(vertexIndex)
            }
        }

        val sizeOfVertex = 3 + 3 + 3 + 4 + 4
        val vertexBuffer = MemoryUtil.memAllocFloat(mesh.mNumVertices() * sizeOfVertex)

        for (i <- 0 until mesh.mNumVertices()) {
            val position = mesh.mVertices().get(i);
			val normal   = mesh.mNormals().get(i);
            //println("position of " + i + ": " + position.x() + " " + position.y + " " + position.z)
            vertexBuffer.put(position.x()).put(position.y()).put(position.z()) //POSITION
                        .put(2f) //material
                        .put(normal.x()).put(normal.y()).put(normal.z()) //NORMAL
                        .put(0.1f) //color var

            val numOfZeroWeights = scala.math.max(0,4 - weightsArray(i).length)

            weightsArray(i) = weightsArray(i).sortWith(_._2 > _._2)

            //JOINT ID
            for ((jointId, weight) <- weightsArray(i)) {
                vertexBuffer.put(jointId.toFloat)
                //print(jointId.toFloat + " ")
            }

            for (j <- 0 until numOfZeroWeights){
                vertexBuffer.put(0f)
                //print(0f + " ")
            }
            //println()
            assert(numOfZeroWeights + weightsArray(i).length == 4)
            //WEIGHT
            for ((jointId, weight) <- weightsArray(i)) {
                vertexBuffer.put(weight); //print(weight + " ")
            }
            for (j <- 0 until numOfZeroWeights) {
                vertexBuffer.put(0f); //print(0f + " ")
            }
            //println()
        }
        vertexBuffer.flip()

        val rootJointNode = findRootJoint(scene.mRootNode(), jointNamesMap.toMap) match {
            case Some(node) => node
            case None => throw new NoSuchElementException("no root joint node found")
        }

        val joints = new Array[Joint](mesh.mNumBones)
        val rootJoint = createJointHierarchy(rootJointNode, mesh, jointNamesMap.toMap, joints)

        //to do: load animation library and set up model vao

        var animationLibrary = scala.collection.mutable.HashMap[String, Animation]()

        for (i <- 0 until scene.mNumAnimations) {
            val animation = AIAnimation.create(scene.mAnimations().get(i))

            val name = animation.mName().dataString()
            println("LOADING ANIMATION " + name)
            val duration = animation.mDuration() / animation.mTicksPerSecond() * 5f

            val keyframeMap = scala.collection.mutable.HashMap[Double, Array[JointTransform]]()

            for (j <- 0 until animation.mNumChannels()) {
                val node = AINodeAnim.create(animation.mChannels.get(j))
                val nodeName = node.mNodeName().dataString()
                if (jointNamesMap contains nodeName) {
                    val jointId = jointNamesMap(nodeName)
                    //POSITIONS
                    for (k <- 0 until node.mNumPositionKeys) {
                        val vectorKey = node.mPositionKeys.get(k)
                        val time = vectorKey.mTime() / animation.mTicksPerSecond() * 5f
                        val position = Vector3(vectorKey.mValue.x, vectorKey.mValue.y, vectorKey.mValue.z)
                        if (keyframeMap contains time) {
                            val oldTransform = keyframeMap(time)(jointId)
                            keyframeMap(time)(jointId) = JointTransform(position, oldTransform.orientation, oldTransform.scaling)
                        }
                        else {
                            val pose = Array.fill[JointTransform](joints.length)(JointTransform())
                            pose(jointId) = JointTransform(position = position)
                            keyframeMap += (time -> pose)
                        }
                    }
                    //ROTATIONS
                    for (k <- 0 until node.mNumRotationKeys) {
                        val quatKey = node.mRotationKeys.get(k)
                        val time = quatKey.mTime()/ animation.mTicksPerSecond() * 5f
                        val orientation = Quaternion(quatKey.mValue.w, quatKey.mValue.x, quatKey.mValue.y, quatKey.mValue.z).normalize

                        if (keyframeMap contains time) {
                            val oldTransform = keyframeMap(time)(jointId)
                            keyframeMap(time)(jointId) =
                                JointTransform(oldTransform.position, orientation, oldTransform.scaling)
                        }
                        else {
                            val pose = new Array[JointTransform](joints.length)
                            pose(jointId) = JointTransform(orientation = orientation)
                            keyframeMap += (time -> pose)
                        }
                    }
                    //SCALINGS
                    for (k <- 0 until node.mNumScalingKeys) {
                        val vectorKey = node.mScalingKeys.get(k)
                        val time = vectorKey.mTime()/ animation.mTicksPerSecond() * 5f
                        val scaling = Vector3(vectorKey.mValue.x, vectorKey.mValue.y, vectorKey.mValue.z)
                        if (keyframeMap contains time) {
                            val oldTransform = keyframeMap(time)(jointId)
                            keyframeMap(time)(jointId) =
                                JointTransform(oldTransform.position, oldTransform.orientation, scaling)
                        }
                        else {
                            val pose = new Array[JointTransform](joints.length)
                            pose(jointId) = JointTransform(scaling = scaling)
                            keyframeMap += (time -> pose)
                        }
                    }

                }
            }
            val keyframes = keyframeMap.toArray.filter(_._1 < duration).sortBy(_._1)
                            .map((p: (Double,Array[JointTransform])) => Keyframe(Pose(p._2), p._1))
            println("num of keyframes " + keyframes.length)
            println("keyframes " + (keyframes map (_.time)).toList)
            println("duration " + duration)
            animationLibrary += (name -> new Animation(keyframes, duration))
        }

        //set up vao
        val vao = glGenVertexArrays()
        glBindVertexArray(vao)

        val vbo = glGenBuffers
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)
        //MemoryUtil.memFree(vertexBuffer)

        val ebo = glGenBuffers
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW)
        //MemoryUtil.memFree(indicesBuffer)

        //vertex positions attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 16*4, 0)
        glEnableVertexAttribArray(0)

        //vertex material id attribute
        glVertexAttribPointer(1, 1, GL_FLOAT, false, 16*4, 3*4)
        glEnableVertexAttribArray(1)

        //vertex normal attribute
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 16*4, 4*4)
        glEnableVertexAttribArray(2)

        //vertex colour variation attribute
        glVertexAttribPointer(3, 1, GL_FLOAT, false, 16*4, 7*4)
        glEnableVertexAttribArray(3)

        //vertex joints attribute
        glVertexAttribPointer(4, 4, GL_FLOAT, false, 16*4, 8*4)
        glEnableVertexAttribArray(4)

        //vertex weights attribute
        glVertexAttribPointer(5, 4, GL_FLOAT, false, 16*4, 12*4)
        glEnableVertexAttribArray(5)

        new AnimatedModel(animationLibrary.toMap, rootJoint, joints, vao, vertexCount)
    }

    private def findRootJoint(node: AINode, jointNamesMap: Map[String, Int]): Option[AINode] = {
        if ((jointNamesMap contains node.mName().dataString()) && !(jointNamesMap contains node.mParent().mName().dataString()))
            Some(node)
        else {
            val children: List[AINode] =
                for (i <- (0 until node.mNumChildren).toList) yield AINode.create(node.mChildren().get(i))

            children.map(findRootJoint(_, jointNamesMap)).flatten match {
                case (x::xs) => Some(x)
                case Nil => None
            }
        }
    }

    private def createJointHierarchy(node: AINode, mesh: AIMesh, jointNamesMap: Map[String, Int],
    joints: Array[Joint]): Joint = {
        val index = jointNamesMap(node.mName().dataString())
        val bone = AIBone.create(mesh.mBones().get(index))
        val inverseBindTransform = Matrix4.fromAssimp(bone.mOffsetMatrix())

        val childrenNodes: List[AINode] =
            for (i <- (0 until node.mNumChildren).toList) yield AINode.create(node.mChildren().get(i))

        val children: List[Joint] =
            for (j <- childrenNodes if jointNamesMap contains j.mName().dataString())
                yield createJointHierarchy(j, mesh, jointNamesMap, joints)

        joints(index) = new Joint(index, children, inverseBindTransform)
        joints(index)
    }
}
