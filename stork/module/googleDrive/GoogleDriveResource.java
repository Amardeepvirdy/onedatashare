package stork.module.googleDrive;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.model.File;
import stork.feather.*;
import stork.feather.errors.NotFound;
import stork.feather.util.ThreadBell;
//import com.dropbox.core.v2.files.*;

import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class GoogleDriveResource extends Resource<GoogleDriveSession, GoogleDriveResource> {

  GoogleDriveResource(GoogleDriveSession session, Path path, String id) {
    super(session, path, id);
  }

  public synchronized Emitter<String> list() {
    final Emitter<String> emitter = new Emitter<>();
    new ThreadBell() {
      public Object run() throws Exception {
//
//        ListFolderResult listing = session.client.files().listFolderContinue(path.toString());
//        for (Metadata child : listing.getEntries())
//          emitter.emit(child.getName());
//        emitter.ring();
        return null;
      }
    }.startOn(initialize());
    return emitter;
  }

  public synchronized Bell<GoogleDriveResource> mkdir() {
    return new ThreadBell<GoogleDriveResource>() {
      @Override
      public GoogleDriveResource run() throws Exception {

        File fileMetadata = new File();
        fileMetadata.setName(path.name());
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = session.service.files().create(fileMetadata)
                .setFields("id")
                .execute();
//        path.
        System.out.println("Folder ID: " + file.getId());
        return new GoogleDriveResource(session, path, file.getId());

      }
    }.startOn(initialize());
  }

  public synchronized Bell<Stat> stat(String folderId) {
    return new ThreadBell<Stat>() {
      public Stat run() throws Exception {
        Drive.Files.List result = null;
        Stat stat = new Stat();
        stat.name = path.name();
        stat.dir = true;
        if(path.toString() == "/") {

          result = session.service.files().list()
                  .setOrderBy("name")
                  .setQ("trashed=false and 'root' in parents")
                  .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");
        }
        else{
          try {
            String query = new StringBuilder().append("trashed=false and ")
//                          .append("'0BzkkzI-oRXwxfjRHVXZxQmhSaldCWWJYX0Y2OVliTkFLbjdzVTBFaWZ5c1RJRF9XSjViQ3c'")
                          .append("'" + folderId + "'")
                          .append(" in parents").toString();

            result = session.service.files().list()
                    .setOrderBy("name")
                    .setQ(query)
                    .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");
          }
          catch (Exception e){
              e.printStackTrace();
          }
        }

        if (result == null)
          throw new NotFound();

        FileList fileSet = null;

        if (stat.dir) {
          List<Stat> sub = new LinkedList<>();
          do {
            try {
              fileSet = result.execute();
              List<File> files = fileSet.getFiles();
              for (File file : files) {
                sub.add(mDataToStat(file));
              }
              stat.setFiles(sub);
              result.setPageToken(fileSet.getNextPageToken());
            }
            catch (NullPointerException e) {

            }
            catch (Exception e) {
              fileSet.setNextPageToken(null);
            }
          }
          while (result.getPageToken() != null);
        }
        return stat;
      }
    }.startOn(initialize());
  }

  public synchronized Bell<Stat> stat() {
    return new ThreadBell<Stat>() {
      public Stat run() throws Exception {
        Drive.Files.List result = null;
        Stat stat = new Stat();
        stat.name = path.name();
        stat.id = id;
        //stat.dir = true;
        if(path.toString() == "/") {
          stat.dir = true;
          result = session.service.files().list()
                  .setOrderBy("name")
                  .setQ("trashed=false and 'root' in parents")
                  .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");

          if (result == null)
            throw new NotFound();

          FileList fileSet = null;

          List<Stat> sub = new LinkedList<>();
          do {
            try {
              fileSet = result.execute();
              List<File> files = fileSet.getFiles();
              for (File file : files) {
                sub.add(mDataToStat(file));
              }
              stat.setFiles(sub);
              result.setPageToken(fileSet.getNextPageToken());
            }
            catch (NullPointerException e) {

            }
            catch (Exception e) {
              fileSet.setNextPageToken(null);
            }
          }
          while (result.getPageToken() != null);
        }
        else{
          try {
            File googleDriveFile = session.service.files().get(id)
                    .setFields("id, name, kind, mimeType, size, modifiedTime").execute();
            if (googleDriveFile.getMimeType().equals("application/vnd.google-apps.folder")) {
              stat.dir = true;

              String query = new StringBuilder().append("trashed=false and ")
//                          .append("'0BzkkzI-oRXwxfjRHVXZxQmhSaldCWWJYX0Y2OVliTkFLbjdzVTBFaWZ5c1RJRF9XSjViQ3c'")
                      .append("'" + id + "'")
                      .append(" in parents").toString();
              result = session.service.files().list()
                      .setOrderBy("name")
                      .setQ(query)
                      .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");
              if (result == null)
                throw new NotFound();

              FileList fileSet = null;

              List<Stat> sub = new LinkedList<>();
              do {
                try {
                  fileSet = result.execute();
                  List<File> files = fileSet.getFiles();
                  for (File file : files) {
                    sub.add(mDataToStat(file));
                  }
                  stat.setFiles(sub);
                  result.setPageToken(fileSet.getNextPageToken());
                }
                catch (NullPointerException e) {

                }
                catch (Exception e) {
                  fileSet.setNextPageToken(null);
                }
              }
              while (result.getPageToken() != null);
            }else {
              System.out.println("A file");
              stat.file = true;
              stat.time = googleDriveFile.getModifiedTime().getValue()/1000;
              stat.size = googleDriveFile.getSize();
            }
          }
          catch (Exception e){
            e.printStackTrace();
          }
        }
        return stat;
      }
    }.startOn(initialize());
  }

  private Stat mDataToStat(File file) {
    Stat stat = new Stat(file.getName());

    try {
      stat.file = true;
      stat.id = file.getId();
      stat.time = file.getModifiedTime().getValue()/1000;
      if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
        stat.dir = true;
        stat.file = false;
      }
      else
        stat.size = file.getSize();
    }
    catch (Exception e) {
//          e.printStackTrace();
    }

    return stat;
  }

  public Tap<GoogleDriveResource> tap() {
    return new GoogleDriveTap();
  }

  public Sink<GoogleDriveResource> sink() {
    return new GoogleDriveSink();
  }

  private class GoogleDriveTap extends Tap<GoogleDriveResource> {
    protected GoogleDriveTap() { super(GoogleDriveResource.this); }

    protected Bell start(Bell bell) {
      return new ThreadBell() {
        public Object run() throws Exception {
          session.service.files().get(id)
                  .executeMediaAndDownloadTo(asOutputStream());
//          session.client.files().download(
//                  source().path.toString());
          finish();
          return null;
        } public void fail(Throwable t) {
          finish();
        }
      }.startOn(initialize().and(bell));
    }
  }

  private class GoogleDriveSink extends Sink<GoogleDriveResource> {
//    private UploadUploader upload;
    File fileMetadata = new File();
    File file;
    Drive.Files.Create create;
    String id;
    InputStream input;
    MediaHttpUploader uploader;
    ByteArrayContent content;

    protected GoogleDriveSink() { super(GoogleDriveResource.this); }

    protected Bell<?> start() {
      return initialize().and((Bell<Stat>)source().stat()).new As<Void>() {
        public Void convert(Stat stat) throws Exception {
          fileMetadata.setName(stat.name);
          String s = "daskagdadjkgjdkfgejkfhifkwegfkwfewf";
          content = new ByteArrayContent("plain/text", s.getBytes(StandardCharsets.UTF_8));
          InputStreamContent mediaContent =
                  new InputStreamContent("image/jpeg",
                          new BufferedInputStream(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))));
          mediaContent.setLength(stat.size);
          create = session.service.files().create(fileMetadata, mediaContent);
          uploader = create.getMediaHttpUploader();
          file = create.setFields("id")
                  .execute();
          id = file.getId();

//          upload = session.client.files().upload(
//                  destination().path.toString());
          return null;
        } public void fail(Throwable t) {
          finish(t);
        }
      };
    }

    protected Bell drain(final Slice slice) {
      return new ThreadBell<Void>() {
        public Void run() throws Exception {
          String uploadURL = "https://www.googleapis.com/upload/drive/v3/files/" + id;
          HttpResponse res = uploader.upload(new GenericUrl(uploadURL));
          //File file1 = session.service.files().update(id, fileMetadata, content).execute();
//          upload.getOutputStream().write(slice.asBytes());
          return null;
        }
      }.start();
    }

    protected void finish(Throwable t) {
      try {
//        upload.finish();
      } catch (Exception e) {
        // Ignore...?
      } finally {
//        upload.close();
      }
    }
  }
}
