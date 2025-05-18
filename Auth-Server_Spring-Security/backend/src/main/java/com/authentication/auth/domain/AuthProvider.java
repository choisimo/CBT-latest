@Entity
@Table(name = "Auth_Provider")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName = "SERVER";
    
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "authProvider", cascade = CascadeType.ALL)
    private List<UserAuthentication> userAuthentications = new ArrayList<>();
}
