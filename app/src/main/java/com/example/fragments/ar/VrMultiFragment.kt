package com.example.fragments.ar

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.aroom.R
import com.example.fragments.shopping.ProductDetailsFragmentArgs
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VrMultiFragment : Fragment(R.layout.fragment_vr) {

    private val args by navArgs<ProductDetailsFragmentArgs>()

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private val storage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference.child("models")

    private var modelsToLoad = emptyList<String>();

    //    private val modelsToLoad = listOf(
//        "1706099350306_chairmodel.glb",
//        "lilly.glb",
//        "monstera.glb"
//    );
    private var currentModelIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modelFilenames = arguments?.getStringArrayList("modelFilenames")
        if (!modelFilenames.isNullOrEmpty()) {
            Log.d(TAG, "Model filenames: $modelFilenames")
            modelsToLoad = modelFilenames
        } else {
            Log.e(TAG, "No model filenames found in arguments")
            // Handle the case where no model filenames are provided
        }

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
            }
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
            }
            setOnTapArPlaneListener(::onTapPlane)
        }
    }


    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        Log.d(TAG, "Model filenames to load: $modelsToLoad")
        if (currentModelIndex <= modelsToLoad.size) {
            val modelUri = modelsToLoad[currentModelIndex]

            lifecycleScope.launch {
                try {
                    val url = modelUri
                    Log.d(TAG, "Model URL: $url")
                    val model = loadModel(url)
                    addToScene(hitResult, model)
                    currentModelIndex++
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading model: ${e.message}")
                }
            }
        } else {
            Toast.makeText(context, "All models loaded", Toast.LENGTH_SHORT).show()
        }
    }


    private suspend fun loadModel(modelUri: String): Renderable {
        return ModelRenderable.builder()
            .setSource(context, Uri.parse(modelUri))
            .setIsFilamentGltf(true)
            .build()
            .await()
    }

//    private fun addToScene(hitResult: HitResult, model: Renderable) {
//        // Create the Anchor.
//        scene.addChild(AnchorNode(hitResult.createAnchor()).apply {
//            // Create the transformable model and add it to the anchor.
//            addChild(TransformableNode(arFragment.transformationSystem).apply {
//                renderable = model
//                localPosition = Vector3(0.0f, 0.0f, 0.0f)
//                localScale = Vector3(0.7f, 0.7f, 0.7f)
//                renderableInstance.setCulling(false)
//                renderableInstance.animate(true).start()
//            })
//        })
//    }

    // Assuming each model has a constant size
    private val MODEL_SIZE = Vector3(0.7f, 0.7f, 0.7f)
    private val PADDING = 500.2f// Adjust as needed

    private fun addToScene(hitResult: HitResult, model: Renderable) {
        val anchorNode = AnchorNode(hitResult.createAnchor())
        scene.addChild(anchorNode)

        val collisionDetected = checkForCollision(anchorNode, MODEL_SIZE)
        if (collisionDetected) {
            // Adjust position to avoid collision
            val newPosition = calculateNonOverlappingPosition(anchorNode.worldPosition, MODEL_SIZE)
            anchorNode.worldPosition = newPosition
        }

        anchorNode.addChild(TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
        })
    }

    private fun checkForCollision(anchorNode: AnchorNode, modelSize: Vector3): Boolean {
        val newBounds = getBoundingBox(anchorNode, modelSize)

        for (existingNode in scene.children) {
            if (existingNode is AnchorNode && existingNode != anchorNode) {
                val existingBounds = getBoundingBox(existingNode, MODEL_SIZE)
                if (doBoundingBoxesIntersect(newBounds, existingBounds)) {
                    return true // Collision detected
                }
            }
        }

        return false // No collision detected
    }

    data class BoundingBox(val min: Vector3, val max: Vector3)

    private fun getBoundingBox(node: AnchorNode, size: Vector3): BoundingBox {
        val worldPosition = node.worldPosition
        val min = Vector3(worldPosition.x - size.x / 2, worldPosition.y - size.y / 2, worldPosition.z - size.z / 2)
        val max = Vector3(worldPosition.x + size.x / 2, worldPosition.y + size.y / 2, worldPosition.z + size.z / 2)
        return BoundingBox(min, max)
    }

    private fun doBoundingBoxesIntersect(box1: BoundingBox, box2: BoundingBox): Boolean {
        return !(box2.min.x > box1.max.x || box2.max.x < box1.min.x ||
                box2.min.y > box1.max.y || box2.max.y < box1.min.y ||
                box2.min.z > box1.max.z || box2.max.z < box1.min.z)
    }



    private fun calculateNonOverlappingPosition(position: Vector3, modelSize: Vector3): Vector3 {
        // Adjust position by adding padding
        return Vector3(
            position.x + modelSize.x + PADDING,
            position.y,
            position.z
        )
    }


    companion object {
        private const val TAG = "VrMultiFragment"
    }
}
