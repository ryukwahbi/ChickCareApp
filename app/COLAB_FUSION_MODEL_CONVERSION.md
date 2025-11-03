# Convert Fusion Model (.h5) to TensorFlow Lite (.tflite)

## Step 1: Add this code to your Google Colab notebook AFTER training

After you've trained your fusion model and saved it as `cnnmlp.h5`, add this conversion code:

```python
import tensorflow as tf
import numpy as np

# Load your trained fusion model
print("📦 Loading trained fusion model...")
model = tf.keras.models.load_model('cnnmlp.h5')

# Print model summary to verify dual inputs
print("\n✅ Model Summary:")
model.summary()

# Verify model has 2 inputs and 1 output
print(f"\n📊 Model Inputs: {len(model.inputs)}")
print(f"📊 Model Outputs: {len(model.outputs)}")

# Convert to TensorFlow Lite
print("\n🔄 Converting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Optimize for mobile (reduces size)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Convert the model
tflite_model = converter.convert()

# Save TFLite model
with open('cnnmlp_fusion.tflite', 'wb') as f:
    f.write(tflite_model)

print(f"\n✅ TFLite model saved!")
print(f"📦 Model size: {len(tflite_model) / 1024 / 1024:.2f} MB")

# Test the TFLite model with dummy inputs
print("\n🧪 Testing TFLite model...")
interpreter = tf.lite.Interpreter(model_content=tflite_model)
interpreter.allocate_tensors()

# Get input/output details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"\n📥 Input Details:")
for i, inp in enumerate(input_details):
    print(f"  Input {i}: shape={inp['shape']}, dtype={inp['dtype']}")

print(f"\n📤 Output Details:")
for i, out in enumerate(output_details):
    print(f"  Output {i}: shape={out['shape']}, dtype={out['dtype']}")

# Test with dummy data (batch_size=1, 224x224x3)
dummy_img = np.random.rand(1, 224, 224, 3).astype(np.float32)
dummy_spec = np.random.rand(1, 224, 224, 3).astype(np.float32)

interpreter.set_tensor(input_details[0]['index'], dummy_img)
interpreter.set_tensor(input_details[1]['index'], dummy_spec)
interpreter.invoke()

output_data = interpreter.get_tensor(output_details[0]['index'])
print(f"\n✅ Test prediction shape: {output_data.shape}")
print(f"✅ Test prediction (probabilities): {output_data[0]}")

# Download the model
print("\n📥 Downloading model...")
from google.colab import files
files.download('cnnmlp_fusion.tflite')

print("\n✅ Conversion complete! Download 'cnnmlp_fusion.tflite' from Colab")
print("📝 Next step: Rename to 'model.tflite' and place in app/src/main/assets/")
```

## Step 2: Download and Replace Model

1. Download `cnnmlp_fusion.tflite` from Colab
2. Copy to: `app/src/main/assets/`
3. **Delete the old `model.tflite`**
4. Rename `cnnmlp_fusion.tflite` to `model.tflite`

## Model Architecture (from your Colab code)

- **Input 1**: Image (224x224x3) - RGB chicken image
- **Input 2**: Spectrogram (224x224x3) - Mel spectrogram from audio
- **Output**: 2 classes (Healthy=0, Unhealthy=1) with softmax probabilities

## Preprocessing Requirements (must match Colab)

1. **Image**: 
   - Resize to 224x224
   - Normalize: `pixel / 255.0` (float32, range 0-1)
   - RGB format

2. **Spectrogram**:
   - Created using librosa: `librosa.feature.melspectrogram()` with 128 mel bands
   - Convert to dB: `librosa.power_to_db()`
   - Save as image and resize to 224x224
   - Normalize: `pixel / 255.0` (float32, range 0-1)
   - RGB format (3 channels)

