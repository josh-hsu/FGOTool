/*
 * Copyright (C) 2016 The Josh Tool Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mumu.libjoshgame;

public class JoshGameLibrary {
    private InputService mInputService;
    private CaptureService mCaptureService;
    private static JoshGameLibrary currentRuntime = new JoshGameLibrary();

    public static JoshGameLibrary getInstance() {
        return currentRuntime;
    }

    private JoshGameLibrary() {
        mCaptureService = new CaptureService();
        mInputService = new InputService(mCaptureService);
    }

    public void SetScreenDimension(int w, int h) {
        mCaptureService.SetScreenDimension(w, h);
        mInputService.SetScreenDimension(w, h);
    }

    public void SetGameOrientation(int orientation) {
        mInputService.SetGameOrientation(orientation);
    }

    public CaptureService getCapSvc() {
        return mCaptureService;
    }

    public InputService getInputSvc() {
        return mInputService;
    }

}
