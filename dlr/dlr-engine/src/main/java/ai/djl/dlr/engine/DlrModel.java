/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.dlr.engine;

import ai.djl.BaseModel;
import ai.djl.Device;
import ai.djl.Model;
import ai.djl.dlr.jni.JniUtils;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.Translator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * {@code DlrModel} is the DLR implementation of {@link Model}.
 *
 * <p>OrtModel contains all the methods in Model to load and process a model. In addition, it
 * provides DLR Specific functionality
 */
public class DlrModel extends BaseModel {

    /**
     * Constructs a new Model on a given device.
     *
     * @param name the model name
     * @param manager the {@link NDManager} to holds the NDArray
     */
    DlrModel(String name, NDManager manager) {
        super(name);
        this.manager = manager;
        this.manager.setName("dlrModel");
        // DLR only support float32
        dataType = DataType.FLOAT32;
    }

    /** {@inheritDoc} */
    @Override
    public void load(Path modelPath, String prefix, Map<String, ?> options) throws IOException {
        modelDir = modelPath.toAbsolutePath();
        if (prefix == null) {
            prefix = modelName;
        }
        if (block != null) {
            throw new UnsupportedOperationException("DLR does not support dynamic blocks");
        }
        checkModelFiles(prefix);
        Device device = manager.getDevice();
        long modelHandle = JniUtils.createDlrModel(modelDir.toString(), device);
        block = new DlrSymbolBlock(modelHandle);
    }

    private void checkModelFiles(String prefix) throws IOException {
        // TODO make the check platform independent
        Path module = modelDir.resolve(prefix + ".dylib");
        if (Files.notExists(module) || !Files.isRegularFile(module)) {
            throw new FileNotFoundException("module file(.so/.dylib/.dll) is missing");
        }
        Path params = modelDir.resolve(prefix + ".params");
        if (Files.notExists(params) || !Files.isRegularFile(module)) {
            throw new FileNotFoundException("params file(.params) is missing");
        }
        Path graph = modelDir.resolve(prefix + ".json");
        if (Files.notExists(graph) || !Files.isRegularFile(graph)) {
            throw new FileNotFoundException("graph file(.json) is missing");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Trainer newTrainer(TrainingConfig trainingConfig) {
        throw new UnsupportedOperationException("Not supported for DlrModel");
    }

    /** {@inheritDoc} */
    @Override
    public <I, O> Predictor<I, O> newPredictor(Translator<I, O> translator) {
        return new Predictor<>(this, translator, false);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getArtifactNames() {
        return new String[0];
    }

    /** {@inheritDoc} */
    @Override
    public void cast(DataType dataType) {
        throw new UnsupportedOperationException("Not supported for DlrModel");
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        manager.close();
    }
}
