package adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apis.MainActivity;
import com.example.apis.R;

import java.util.ArrayList;
import java.util.List;
import beans.Contact;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private Context context;
    private List<Contact> contacts;
    private List<Contact> contactsFull;
    private int[] letterColors;

    public ContactAdapter(Context context, List<Contact> contacts) {
        this.context = context;
        this.contacts = contacts;
        this.contactsFull = new ArrayList<>(contacts);
        this.letterColors = context.getResources().getIntArray(R.array.letter_colors);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.nameTextView.setText(contact.getName());
        holder.phoneTextView.setText(contact.getNumber());

        // Set first letter tag
        String name = contact.getName();
        if (!name.isEmpty()) {
            char firstLetter = Character.toUpperCase(name.charAt(0));
            holder.letterTag.setText(String.valueOf(firstLetter));

            // Set dynamic color based on letter
            int colorIndex = Math.abs(firstLetter) % letterColors.length;
            holder.letterTag.setBackgroundTintList(ColorStateList.valueOf(letterColors[colorIndex]));
        } else {
            holder.letterTag.setText("?");
            holder.letterTag.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.default_tag_color)));
        }

        holder.itemView.setOnClickListener(v -> {
            ((MainActivity)context).showContactActionsDialog(contact.getName(), contact.getNumber());
        });
    }
    public void filter(String text) {
        contacts.clear();
        if (text.isEmpty()) {
            contacts.addAll(contactsFull);
        } else {
            text = text.toLowerCase();
            for (Contact contact : contactsFull) {
                if (contact.getName().toLowerCase().contains(text) ||
                        contact.getNumber().contains(text)) {
                    contacts.add(contact);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Add this method to update the full list when data changes
    public void updateFullList(List<Contact> newContacts) {
        contactsFull.clear();
        contactsFull.addAll(newContacts);
        filter(""); // Reset filter
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView phoneTextView;
        TextView letterTag;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contact_name);
            phoneTextView = itemView.findViewById(R.id.contact_phone);
            letterTag = itemView.findViewById(R.id.letterTag);
        }
    }
}