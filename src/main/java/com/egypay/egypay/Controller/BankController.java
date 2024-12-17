package com.egypay.egypay.Controller;

import com.egypay.egypay.Config.Mapper;
import com.egypay.egypay.Models.DTO.*;
import com.egypay.egypay.Models.Entities.BankEntity;
import com.egypay.egypay.Services.BankServiceINT;
import com.egypay.egypay.Services.FavBankService;
import com.egypay.egypay.Services.UserServiceINT;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BankController {

   private final UserServiceINT userService;
   private final BankServiceINT bankService;
   private final FavBankService favBankService;

    @RequestMapping("/")
    public String logIn(Model model) {
        if(!model.containsAttribute("user")) {
            model.addAttribute("user", new UserDTO(0L, "", "", "", "",0D));
        }
            return "LogIn";
    }

    @RequestMapping("/Login")
    public String findUserByEmail(@ModelAttribute("user") UserDTO userDto , RedirectAttributes redirectAttributes) {
        if(userService.FindUserByEmail(userDto)){
            redirectAttributes.addFlashAttribute("user", userDto);
            if(userDto.getEmail().contains("_bk_"))
            {
                return "redirect:/AdminHome";
            }else
            {
                return "redirect:/Home";
            }

        }
        return "redirect:/RegForm";
    }
    @RequestMapping("/RegForm")
    public String Regform(@ModelAttribute("user") UserDTO userDto)
    {
        return "Reg";
    }
    @RequestMapping("/RegisterForm")
    public String Reg(@ModelAttribute("user") UserDTO user , RedirectAttributes redirectAttributes)
    {
        redirectAttributes.addFlashAttribute("user", user);
        return "redirect:/Register";
    }

    @RequestMapping("/Register")
    public String Main(@ModelAttribute("user") UserDTO user)
    {
        if(userService.saveUser(user))
        {
            return "redirect:/";

        }else
        {
            return "Fail";
        }
    }
    @RequestMapping("/Home")
    public String Home(Model model , @ModelAttribute("user") UserDTO userDto , RedirectAttributes redirectAttributes)
    {
        model.addAttribute("fullname", userService.findUserEntityByEmail(userDto.getEmail()).getFullName());
        redirectAttributes.addFlashAttribute("user", userDto);
        return "index";
    }
    @RequestMapping("/AdminHome")
    public String AdminHome(Model model , @ModelAttribute("user") UserDTO userDto , RedirectAttributes redirectAttributes)
    {
        model.addAttribute("fullname", userService.findUserEntityByEmail(userDto.getEmail()).getFullName());
        redirectAttributes.addFlashAttribute("user",userDto);
        return "Adminindex";
    }
    @RequestMapping("/BankMoney")
    public String BankMoney(@ModelAttribute("Bank") BankDTO bankDTO, Model model , RedirectAttributes redirectAttributes)
    {

        model.addAttribute("Bank", new BankDTO(0L,null,null,null,0,null));
        Double balance = bankService.findBankEntitiesByName("EGY2").getBalance();
        model.addAttribute("balance", balance);
        return "SendMoney";
    }
    @Transactional
    @RequestMapping("/SendMoney")
    public String SendMoney(@ModelAttribute("Bank") BankDTO bankDTO , Model model , RedirectAttributes redirectAttributes)
    {
        double Mybalance = 0D;
        Mapper mapper = new Mapper();
        // Fetch the bank balance using the provided SWIFT code
        String SwiftCode = bankDTO.getSwift();
        // Logic for transferring money
        Mybalance = bankService.findBankEntityBySwift(bankDTO.getMySwift()).getBalance();
        Integer Amount =bankDTO.getBalance();
        if (Amount <= (int)Mybalance) {
            Mybalance -= Amount;
            BankEntity bankEntity = bankService.findBankEntityBySwift(bankDTO.getMySwift());
            bankEntity.setBalance(Mybalance);
            System.out.println(Amount);
            RequestBodyDTO requestBody = new RequestBodyDTO();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("token", "BOE-0112-XgF0");
            //requestBody.setRequestId("BOE-0112-XgF0");
            String targetURL = "http://172.21.2.52:8080/deposit";
            requestBody.setAmount(String.valueOf(Amount));
            HttpEntity<RequestBodyDTO> entity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = new RestTemplate();
            System.out.println(requestBody.getAmount());
            ResponseEntity<RequestBodyDTO> responseEntity = restTemplate.exchange(targetURL, HttpMethod.POST, entity, RequestBodyDTO.class);
            System.out.println(responseEntity);
            System.out.println(Mybalance);
            bankService.save(bankEntity);

           /* BankEntity bankEntity2 = bankService.findBankEntityBySwift(SwiftCode);
            bankEntity2.setBalance(bankEntity2.getBalance() + Amount);
            bankService.save(mapper.getmap().map(bankEntity2, BankEntity.class));
        */

        }

        redirectAttributes.addFlashAttribute("balance", Mybalance);
        return "redirect:/BankMoney";
    }

    @RequestMapping("/Recive")
    public String  Recive(@RequestBody RequestBodyDTO requestBodyDTO, RedirectAttributes redirectAttributes)
    {
        double Mybalance = 0D;
        Mapper mapper = new Mapper();
        BankDTO bankDTO = new BankDTO();

        Mybalance = bankService.findBankEntityBySwift(bankDTO.getMySwift()).getBalance();
        System.out.println(Mybalance);
        BankEntity bankEntity2 = bankService.findBankEntityBySwift(bankDTO.getMySwift());
        Double NewBalance =bankEntity2.getBalance() + Double.valueOf(requestBodyDTO.getAmount());
        bankEntity2.setBalance(NewBalance);
        bankService.save(bankEntity2);
        System.out.println(Mybalance);
        redirectAttributes.addFlashAttribute("balance", NewBalance);
        return "redirect:/BankMoney";
    }
    @RequestMapping("/ReciveRequest")
    public void   ReciveRequest(@RequestBody RequestBodyDTO requestBodyDTO, RedirectAttributes redirectAttributes)
    {
        double Mybalance = 0D;
        Mapper mapper = new Mapper();
        BankDTO bankDTO = new BankDTO();

        Mybalance = bankService.findBankEntityBySwift(bankDTO.getMySwift()).getBalance();
        System.out.println(Mybalance);
        BankEntity bankEntity2 = bankService.findBankEntityBySwift(bankDTO.getMySwift());
        Double NewBalance =bankEntity2.getBalance() - Double.valueOf(requestBodyDTO.getAmount());
        bankEntity2.setBalance(NewBalance);
        bankService.save(bankEntity2);
        System.out.println(Mybalance);


        RequestBodyDTO requestBody = new RequestBodyDTO();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("token", "BOE-0112-XgF0");
        //requestBody.setRequestId("BOE-0112-XgF0");
        requestBody.setAmount(requestBodyDTO.getAmount());
        String targetURL = "http://192.168.137.213:8083/deposit";
        HttpEntity<RequestBodyDTO> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RequestBodyDTO> responseEntity = restTemplate.exchange(targetURL, HttpMethod.POST, entity, RequestBodyDTO.class);
       // RequestBodyDTO responseBody = restTemplate.postForObject(targetURL,requestBody,RequestBodyDTO.class);

        redirectAttributes.addFlashAttribute("balance", NewBalance);

    }

}

