package com.adihascal.clipboardsync.util;

import java.io.ByteArrayOutputStream;

class RLECompressor
{
    private ByteArrayOutputStream bas = new ByteArrayOutputStream();
    private byte[] datbuf = new byte[256];
    private byte srledat = 0;
    private int rleoffs = 0;
    private int srleoffs = 0;
    private int datpos = 0;


    private void writeRLE()
    {
        this.bas.write((byte) this.rleoffs);
        this.datpos = 0;
        this.rleoffs = 0;
        this.srleoffs = 0;
    }

    private void writeSRLE()
    {
        this.bas.write(-1);
        this.bas.write((byte) this.srleoffs);
        this.bas.write(this.srledat);
        this.datpos = 0;
        this.rleoffs = 0;
        this.srleoffs = 0;
    }

    private void writeDat(int bytes)
    {
        if (bytes != 0)
        {
            this.bas.write((byte) (128 | bytes));
            this.bas.write(this.datbuf, 0, bytes);
            this.datpos -= bytes;
        }
    }

    void addByte(byte b)
    {
        if (this.rleoffs > 5 && this.rleoffs >= this.srleoffs)
        {
            this.writeDat(this.datpos - this.rleoffs);
            this.writeRLE();
        }

        this.rleoffs = 0;

        if (this.srleoffs == 0)
        {
            this.srledat = b;
            this.srleoffs = 1;
        }
        else if (b == this.srledat)
        {
            ++this.srleoffs;
            if (this.srleoffs >= 127)
            {
                ++this.datpos;
                this.writeDat(this.datpos - this.srleoffs);
                this.writeSRLE();
                return;
            }
        }
        else
        {
            if (this.srleoffs > 5 && this.srleoffs >= this.rleoffs)
            {
                this.writeDat(this.datpos - this.srleoffs);
                this.writeSRLE();
            }

            this.srledat = b;
            this.srleoffs = 1;
        }

        this.datbuf[this.datpos] = b;
        ++this.datpos;
        int rem = Math.max(this.srleoffs, this.rleoffs);
        if (rem <= 5 && this.datpos >= 126)
        {
            this.writeDat(this.datpos);
            this.srleoffs = 0;
            this.rleoffs = 0;
        }
        else if (this.datpos - rem >= 126)
        {
            this.writeDat(this.datpos - rem);
        }

    }

    void flush()
    {
        this.datpos -= this.rleoffs;
        this.srleoffs = Math.max(0, this.srleoffs - this.rleoffs);
        if (this.datpos != 0)
        {
            if (this.srleoffs > 5)
            {
                this.writeDat(this.datpos - this.srleoffs);
                this.writeSRLE();
            }
            else
            {
                this.writeDat(this.datpos);
            }

        }
    }

    byte[] getByteArray()
    {
        return this.bas.toByteArray();
    }
}
