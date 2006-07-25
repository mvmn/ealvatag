/**
 *  Amended @author : Paul Taylor
 *  Initial @author : Eric Farng
 *
 *  Version @version:$Id$
 *
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Description:
 *
 */
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.tag.datatype.*;
import org.jaudiotagger.tag.InvalidTagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;
import org.jaudiotagger.tag.id3.valuepair.Languages;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


public class FrameBodyUSER extends AbstractID3v2FrameBody implements ID3v24FrameBody
{
    /**
     * Creates a new FrameBodyUSER datatype.
     */
    public FrameBodyUSER()
    {
        //        setObject("Text Encoding", new Byte((byte) 0));
        //        setObject("Language", "");
        //        setObject("Text", "");
    }

    public FrameBodyUSER(FrameBodyUSER body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyUSER datatype.
     *
     * @param textEncoding DOCUMENT ME!
     * @param language     DOCUMENT ME!
     * @param text         DOCUMENT ME!
     */
    public FrameBodyUSER(byte textEncoding, String language, String text)
    {
        setObjectValue(DataTypes.OBJ_TEXT_ENCODING, new Byte(textEncoding));
        setObjectValue(DataTypes.OBJ_LANGUAGE, language);
        setObjectValue(DataTypes.OBJ_TEXT, text);
    }

    /**
     * Creates a new FrameBodyUSER datatype.
     *
     * @throws IOException         DOCUMENT ME!
     * @throws InvalidTagException DOCUMENT ME!
     */
    public FrameBodyUSER(ByteBuffer byteBuffer, int frameSize)
        throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

    /**
      * The ID3v2 frame identifier
      *
      * @return the ID3v2 frame identifier  for this frame type
     */
    public String getIdentifier()
    {
        return ID3v24Frames.FRAME_ID_TERMS_OF_USE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getLanguage()
    {
        return (String) getObjectValue(DataTypes.OBJ_LANGUAGE);
    }

    /**
     * DOCUMENT ME!
     *
     * @param language DOCUMENT ME!
     */
    public void setOwner(String language)
    {
        setObjectValue(DataTypes.OBJ_LANGUAGE, language);
    }

    /** If the text cannot be encoded using current encoder, change the encoder */
    public void write(ByteArrayOutputStream tagBuffer)
        throws IOException
    {
        if (((AbstractString) getObject(DataTypes.OBJ_TEXT)).canBeEncoded() == false)
        {
            this.setTextEncoding(TextEncoding.UTF_16BE);
        }
        super.write(tagBuffer);
    }

    /**
     * DOCUMENT ME!
     */
    protected void setupObjectList()
    {
        objectList.add(new NumberHashMap(DataTypes.OBJ_TEXT_ENCODING, this, TextEncoding.TEXT_ENCODING_FIELD_SIZE));
        objectList.add(new StringHashMap(DataTypes.OBJ_LANGUAGE, this, Languages.LANGUAGE_FIELD_SIZE));
        objectList.add(new StringSizeTerminated(DataTypes.OBJ_TEXT, this));
    }
}
