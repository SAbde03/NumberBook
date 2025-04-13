package retrofit;

import beans.Contact;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/contacts")
    Call<Contact> createContact(@Body Contact contact);
}
