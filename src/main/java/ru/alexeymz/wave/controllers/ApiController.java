package ru.alexeymz.wave.controllers;

import com.google.common.io.LittleEndianDataOutputStream;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @RequestMapping(method = RequestMethod.GET, value = "/endless/{baseFrequency}", produces = "audio/wav")
    public void generateEndlessLowering(@PathVariable int baseFrequency, HttpServletResponse response) throws Exception {
        generateWaveform(baseFrequency, response.getOutputStream());
    }

    private void generateWaveform(double baseFrequency, OutputStream os) throws IOException {
        int RIFF = 0x46464952;
        int WAVE = 0x45564157;
        int formatChunkSize = 16;
        int headerSize = 8;
        int format = 0x20746D66;
        short formatType = 1;
        short tracks = 1;
        int samplesPerSecond = 44100;
        short bitsPerSample = 16;
        short frameSize = (short)(tracks * ((bitsPerSample + 7) / 8));
        int bytesPerSecond = samplesPerSecond * frameSize;
        int waveSize = 4;
        int data = 0x61746164;
        int T = 16;
        int reps = 3;
        int samplesperrep = samplesPerSecond * T;
        int dataChunkSize = samplesperrep * reps * frameSize;
        int fileSize = waveSize + headerSize + formatChunkSize + headerSize + dataChunkSize;

        try (LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(os)) {
            dos.writeInt(RIFF);
            dos.writeInt(fileSize);
            dos.writeInt(WAVE);
            dos.writeInt(format);
            dos.writeInt(formatChunkSize);
            dos.writeShort(formatType);
            dos.writeShort(tracks);
            dos.writeInt(samplesPerSecond);
            dos.writeInt(bytesPerSecond);
            dos.writeShort(frameSize);
            dos.writeShort(bitsPerSample);
            dos.writeInt(data);
            dos.writeInt(dataChunkSize);

            double fundamental = baseFrequency;
            double ampl = 10000;
            for (int j = 0; j < reps; ++j) {
                for (int i = 0; i < samplesperrep; i++) {
                    double t = (double)i / (double)samplesPerSecond;
                    double amp0 = (ampl / 8) * t / T;
                    double amp1 = (ampl / 8) + (ampl / 8) * t / T;
                    double amp2 = (ampl / 4) + (ampl / 4) * t / T;
                    double amp3 = (ampl / 2) + (ampl / 2) * t / T;
                    double amp4 = (ampl / 1) - (ampl / 2) * t / T;
                    double amp5 = (ampl / 2) - (ampl / 4) * t / T;
                    double amp6 = (ampl / 4) - (ampl / 8) * t / T;
                    double amp7 = (ampl / 8) - (ampl / 8) * t / T;

                    double theta = -fundamental * 2 * Math.PI * T * Math.pow(2, -t / T) / Math.log(2);
                    short s = (short)(
                        amp0 * Math.sin(theta * 16) + amp1 * Math.sin(theta * 8) +
                        amp2 * Math.sin(theta * 4) + amp3 * Math.sin(theta * 2) +
                        amp4 * Math.sin(theta * 1) + amp5 * Math.sin(theta / 2) +
                        amp6 * Math.sin(theta / 4) + amp7 * Math.sin(theta / 8)
                    );
                    dos.writeShort(s);
                }
            }
        }
    }
}
