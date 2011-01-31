/*
 * Copyright 2011 @ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.ashigeru.util.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * テスト用に強制的にクラスを再ロードする。
 */
public class RedefiningClassLoader extends ClassLoader {

    private Map<String, Class<?>> redefined = new HashMap<String, Class<?>>();

    /**
     * インスタンスを生成する。
     * @param parent 親クラスローダー
     * @throws IllegalArgumentException 引数に{@code null}が含まれる場合
     */
    public RedefiningClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected synchronized Class<?> loadClass(
            String name,
            boolean resolve) throws ClassNotFoundException {
        Class<?> aClass = redefined.get(name);
        if (aClass != null) {
            return aClass;
        }
        return super.loadClass(name, resolve);
    }

    /**
     * 指定のクラスをこのクラスローダーで強制的に再定義する。
     * @param aClass 対象のクラス
     * @return 再定義したクラス
     * @throws ClassNotFoundException クラスのロードに失敗した場合
     * @throws IllegalArgumentException 引数に{@code null}が含まれる場合
     */
    public synchronized Class<?> redefine(Class<?> aClass) throws ClassNotFoundException {
        if (aClass == null) {
            throw new IllegalArgumentException("aClass must not be null"); //$NON-NLS-1$
        }
        Class<?> redef = redefined.get(aClass.getName());
        if (redef == null) {
            redef = forceLoad(aClass.getName());
            redefined.put(aClass.getName(), redef);
        }
        return redef;
    }

    private Class<?> forceLoad(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/') + ".class";
        InputStream in = getParent().getResourceAsStream(internalName);
        if (in == null) {
            throw new ClassNotFoundException(name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (true) {
                int read = in.read(buf);
                if (read < 0) {
                    break;
                }
                out.write(buf, 0, read);
            }
            byte[] contents = out.toByteArray();
            return defineClass(name, contents, 0, contents.length, null);
        }
        catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException e) {
                // ignored.
            }
        }
    }
}
